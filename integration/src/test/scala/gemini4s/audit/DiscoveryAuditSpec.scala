package gemini4s.audit

import cats.effect.IO
import munit.CatsEffectSuite
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import gemini4s.config.ApiKey
import gemini4s.http.GeminiHttpClient
import gemini4s._
import gemini4s.audit.ApiMapping
import gemini4s.model.domain.ModelCapabilities._
import io.circe.Json
import io.circe.parser._
import scala.io.Source
import java.io.File

class DiscoveryAuditSpec extends CatsEffectSuite {

  // Define structure for config
  case class IgnoreItem(name: String, reason: String, link: String)
  case class ResourceConfig(implemented: List[String], ignored: List[IgnoreItem])
  case class ModelsConfig(tracked: List[String], ignored: List[IgnoreItem])
  case class ApiVersionsConfig(supported: List[String], ignored: List[IgnoreItem])

  case class Config(
      apiVersions: Option[ApiVersionsConfig],
      resources: Map[String, ResourceConfig],
      schemas: Map[String, ResourceConfig],
      availableModels: Option[ModelsConfig]
  )

  // Model capability assertions - maps model name patterns to expected capabilities
  // NOTE: streamGenerateContent is NOT listed in supportedGenerationMethods but IS available
  // When generateContent is supported, streaming is implicitly available via the :streamGenerateContent endpoint
  case class ModelCapabilityAssertion(
      modelPattern: String,
      expectedMethods: Set[String],
      description: String
  )

  // Our capability model assertions - generated programmatically from Model definitions
  // This ensures that our code's understanding of model capabilities matches reality
  val capabilityAssertions = {
    val modelObj = Model
    modelObj.getClass.getDeclaredMethods
      .filter(_.getParameterCount == 0)
      .filter(m => classOf[Model[?]].isAssignableFrom(m.getReturnType))
      .map { m =>
        m.setAccessible(true)
        val model = m.invoke(modelObj).asInstanceOf[Model[?]]
        ModelCapabilityAssertion(
          model.value, // Uses the model name as the pattern (exact match or prefix)
          model.requiredMethods,
          s"Defined Model: ${m.getName}"
        )
      }
      .toList
  }

  // Verify we actually generated assertions
  test("Code Awareness Sanity Check") {
    assert(capabilityAssertions.nonEmpty, "Failed to generate capability assertions from Model object via reflection")
  }

  import io.circe._
  implicit val ignoreItemDecoder: Decoder[IgnoreItem] = Decoder.forProduct3("name", "reason", "link")(IgnoreItem.apply)

  implicit val resourceConfigDecoder: Decoder[ResourceConfig] =
    Decoder.forProduct2("implemented", "ignored")(ResourceConfig.apply)

  implicit val modelsConfigDecoder: Decoder[ModelsConfig] =
    Decoder.forProduct2("tracked", "ignored")(ModelsConfig.apply)

  implicit val apiVersionsConfigDecoder: Decoder[ApiVersionsConfig] =
    Decoder.forProduct2("supported", "ignored")(ApiVersionsConfig.apply)

  implicit val configDecoder: Decoder[Config] =
    Decoder.forProduct4("apiVersions", "resources", "schemas", "availableModels")(Config.apply)

  val apiKey = sys.env.get("GEMINI_API_KEY")

  test("Automated API Compliance Audit") {
    val configFile = new File("src/test/resources/compliance_config.json")

    if (!configFile.exists()) {
      fail(s"Config file not found at: ${configFile.getAbsolutePath}")
    }

    val configJson = Source.fromFile(configFile).mkString
    val config     = decode[Config](configJson).fold(
      err => throw new Exception(s"JSON Decode Error: $err"),
      c => c
    )

    apiKey match {
      case Some(key) => HttpClientFs2Backend.resource[IO]().use { backend =>
          val httpClient = GeminiHttpClient.make[IO](
            backend,
            ApiKey.unsafe(key),
            baseUrl = "https://generativelanguage.googleapis.com"
          )

          httpClient.get[Json]("$discovery/rest", Map("version" -> "v1beta")).map {
            case Right(discovery) =>
              // --- Audit Resources & Methods ---
              val resources = discovery.hcursor.downField("resources").keys.getOrElse(Iterable.empty)

              val resourceErrors = resources.toList.flatMap { resName =>
                config.resources.get(resName).toList.flatMap { conf =>
                  val methodsCursor = discovery.hcursor.downField("resources").downField(resName).downField("methods")
                  val apiMethods    = methodsCursor.keys.getOrElse(Iterable.empty).toList
                  val tracked       = conf.implemented ++ conf.ignored.map(_.name)
                  val missing       = apiMethods.diff(tracked)
                  if (missing.nonEmpty)
                    List(s"Resource '$resName' has unimplemented/untracked methods: ${missing.mkString(", ")}")
                  else Nil
                }
              }

              // --- Audit Schemas (GenerationConfig) ---
              val genConfigProps =
                discovery.hcursor.downField("schemas").downField("GenerationConfig").downField("properties")
              val apiProps       = genConfigProps.keys.getOrElse(Iterable.empty).toList

              val schemaErrors = config.schemas.get("GenerationConfig").toList.flatMap { confSchema =>
                val trackedProps = confSchema.implemented ++ confSchema.ignored.map(_.name)
                val missingProps = apiProps.diff(trackedProps)
                if (missingProps.nonEmpty)
                  List(s"Schema 'GenerationConfig' has untracked fields: ${missingProps.mkString(", ")}")
                else Nil
              }

              val errors = resourceErrors ++ schemaErrors
              if (errors.nonEmpty) {
                fail(
                  s"Compliance Audit Failed:\n${errors.mkString("\n")}\n\nPlease update compliance_config.json with 'ignored' entries if these are known missing features."
                )
              } else {
                println("Compliance Audit Passed: All API features are tracked.")
              }

            case Left(e) => fail(s"Failed to fetch discovery document: $e")
          }
        }
      case None      => IO(println("No API key, skipping compliance audit"))
    }
  }

  test("Model Capabilities Verification") {
    apiKey match {
      case Some(key) => HttpClientFs2Backend.resource[IO]().use { backend =>
          val httpClient = GeminiHttpClient.make[IO](
            backend,
            ApiKey.unsafe(key),
            baseUrl = "https://generativelanguage.googleapis.com"
          )

          // Fetch the models list to get actual supportedGenerationMethods
          httpClient.get[Json]("v1beta/models").map {
            case Right(modelsJson) =>
              val models   = modelsJson.hcursor.downField("models").as[List[Json]].getOrElse(List.empty)
              var errors   = List.empty[String]
              var verified = List.empty[String]

              capabilityAssertions.foreach { assertion =>
                // Find models matching the pattern
                val matchingModels = models.filter { m =>
                  val name = m.hcursor.downField("name").as[String].getOrElse("")
                  name.contains(assertion.modelPattern) && !name.contains("native-audio")
                }

                if (matchingModels.isEmpty) {
                  // Model not found - could be renamed or removed
                  println(s"WARNING: No models found matching pattern '${assertion.modelPattern}'")
                } else {
                  matchingModels.foreach { model =>
                    val modelName        = model.hcursor.downField("name").as[String].getOrElse("unknown")
                    val supportedMethods =
                      model.hcursor.downField("supportedGenerationMethods").as[List[String]].getOrElse(List.empty).toSet

                    // Check if all expected methods are supported
                    val missingMethods = assertion.expectedMethods.diff(supportedMethods)
                    if (missingMethods.nonEmpty) {
                      errors :+= s"${assertion.description}: Model '$modelName' is missing expected methods: ${missingMethods
                          .mkString(", ")}. Actual: ${supportedMethods.mkString(", ")}"
                    } else {
                      verified :+= s"✓ $modelName supports all expected methods for ${assertion.description}"
                    }
                  }
                }
              }

              // Print verified models
              verified.foreach(println)

              if (errors.nonEmpty) {
                fail(
                  s"Model Capability Verification Failed:\n${errors.mkString("\n")}\n\nUpdate ModelCapabilities.scala to match actual API capabilities."
                )
              } else {
                println("Model Capabilities Verified: All assertions match API reality.")
              }

            case Left(e) => fail(s"Failed to fetch models: $e")
          }
        }
      case None      => IO(println("No API key, skipping model capabilities verification"))
    }
  }

  test("Available Models Audit") {
    val configFile = new File("src/test/resources/compliance_config.json")
    if (!configFile.exists()) {
      fail(s"Config file not found at: ${configFile.getAbsolutePath}")
    }

    val configJson = Source.fromFile(configFile).mkString
    val config     = decode[Config](configJson).fold(
      err => throw new Exception(s"JSON Decode Error: $err"),
      c => c
    )

    apiKey match {
      case Some(key) => HttpClientFs2Backend.resource[IO]().use { backend =>
          val httpClient = GeminiHttpClient.make[IO](
            backend,
            ApiKey.unsafe(key),
            baseUrl = "https://generativelanguage.googleapis.com"
          )

          httpClient.get[Json]("v1beta/models").map {
            case Right(modelsJson) =>
              val models = modelsJson.hcursor.downField("models").as[List[Json]].getOrElse(List.empty)

              // Extract model names (strip "models/" prefix for cleaner config)
              val apiModels = models.flatMap { m =>
                m.hcursor.downField("name").as[String].toOption.map(_.stripPrefix("models/"))
              }.toSet

              config.availableModels match {
                case Some(modelsConfig) =>
                  val tracked   = (modelsConfig.tracked ++ modelsConfig.ignored.map(_.name)).toSet
                  val untracked = apiModels.diff(tracked)

                  if (untracked.nonEmpty) {
                    println(s"\n=== UNTRACKED MODELS (${untracked.size}) ===")
                    untracked.toList.sorted.foreach(m => println(s"  - $m"))
                    fail(
                      s"Found ${untracked.size} untracked models:\n${untracked.toList.sorted.mkString("\n")}\n\nAdd these to 'availableModels.tracked' or 'availableModels.ignored' in compliance_config.json"
                    )
                  } else {
                    println(s"Available Models Audit Passed: All ${apiModels.size} models are tracked.")
                  }

                case None =>
                  // No availableModels section - print all models for initial setup
                  println(s"\n=== AVAILABLE MODELS (${apiModels.size}) ===")
                  println("Add 'availableModels' section to compliance_config.json with these models:")
                  println(s""""availableModels": {""")
                  println(s"""  "tracked": [""")
                  apiModels.toList.sorted.foreach(m => println(s"""    "$m","""))
                  println(s"""  ],""")
                  println(s"""  "ignored": []""")
                  println(s"""}""")
                  // Don't fail - just warn
                  println("\nWARNING: No 'availableModels' section in config. Add one to track new models.")
              }

            case Left(e) => fail(s"Failed to fetch models: $e")
          }
        }
      case None      => IO(println("No API key, skipping available models audit"))
    }
  }

  test("API Version Audit") {
    val configFile = new File("src/test/resources/compliance_config.json")
    if (!configFile.exists()) {
      fail(s"Config file not found at: ${configFile.getAbsolutePath}")
    }

    val configJson = Source.fromFile(configFile).mkString
    val config     = decode[Config](configJson).fold(
      err => throw new Exception(s"JSON Decode Error: $err"),
      c => c
    )

    apiKey match {
      case Some(key) => HttpClientFs2Backend.resource[IO]().use { backend =>
          val httpClient = GeminiHttpClient.make[IO](
            backend,
            ApiKey.unsafe(key),
            baseUrl = "https://generativelanguage.googleapis.com"
          )

          // Check both v1 and v1beta to understand differences
          for {
            v1betaResult <- httpClient.get[Json]("$discovery/rest", Map("version" -> "v1beta"))
            v1Result     <- httpClient.get[Json]("$discovery/rest", Map("version" -> "v1"))
          } yield (v1betaResult, v1Result) match {
            case (Right(v1beta), Right(v1)) =>
              val v1betaVersion = v1beta.hcursor.downField("version").as[String].getOrElse("unknown")
              val v1Version     = v1.hcursor.downField("version").as[String].getOrElse("unknown")

              println(s"\n=== API VERSION AUDIT ===")
              println(s"v1beta version: $v1betaVersion")
              println(s"v1 (stable) version: $v1Version")

              // Compare resources between versions
              val v1betaResources = v1beta.hcursor.downField("resources").keys.getOrElse(Iterable.empty).toSet
              val v1Resources     = v1.hcursor.downField("resources").keys.getOrElse(Iterable.empty).toSet

              val onlyInBeta = v1betaResources.diff(v1Resources)
              val onlyInV1   = v1Resources.diff(v1betaResources)

              if (onlyInBeta.nonEmpty) {
                println(s"\nResources only in v1beta: ${onlyInBeta.mkString(", ")}")
              }
              if (onlyInV1.nonEmpty) {
                println(s"\nResources only in v1: ${onlyInV1.mkString(", ")}")
              }

              // Compare models resource methods
              def getMethods(json: Json, resource: String): Set[String] = json.hcursor
                .downField("resources")
                .downField(resource)
                .downField("methods")
                .keys
                .getOrElse(Iterable.empty)
                .toSet

              val v1betaModelMethods = getMethods(v1beta, "models")
              val v1ModelMethods     = getMethods(v1, "models")

              val methodsOnlyInBeta = v1betaModelMethods.diff(v1ModelMethods)
              val methodsOnlyInV1   = v1ModelMethods.diff(v1betaModelMethods)

              if (methodsOnlyInBeta.nonEmpty) {
                println(s"\nmodels methods only in v1beta: ${methodsOnlyInBeta.mkString(", ")}")
              }
              if (methodsOnlyInV1.nonEmpty) {
                println(s"\nmodels methods only in v1: ${methodsOnlyInV1.mkString(", ")}")
              }

              // Verify config tracks API versions
              config.apiVersions match {
                case Some(versions) =>
                  println(s"\nConfigured to use: ${versions.supported.mkString(", ")}")
                  println(s"Ignored versions: ${versions.ignored.map(_.name).mkString(", ")}")

                  if (!versions.supported.contains("v1beta")) {
                    fail("Configuration should include 'v1beta' in supported versions")
                  }

                  // Verify our ApiVersion feature mappings are accurate
                  import gemini4s.config.ApiVersion

                  val expectedV1BetaResources = ApiVersion.v1betaOnlyResources
                  val expectedV1Resources     = ApiVersion.v1OnlyResources

                  val missingFromMapping = onlyInBeta.diff(expectedV1BetaResources)
                  val extraInMapping     = expectedV1BetaResources.diff(onlyInBeta)

                  if (missingFromMapping.nonEmpty) {
                    println(
                      s"\nWARNING: Resources in v1beta but not in ApiVersion.v1betaOnlyResources: ${missingFromMapping.mkString(", ")}"
                    )
                  }
                  if (extraInMapping.nonEmpty) {
                    println(
                      s"\nWARNING: Resources in ApiVersion.v1betaOnlyResources but not in v1beta: ${extraInMapping.mkString(", ")}"
                    )
                  }

                  println("\nAPI Version Audit Passed: v1beta is configured and available.")

                case None => println("\nWARNING: No 'apiVersions' section in config. Add one to track API versions.")
              }

            case (Left(e1), _) => fail(s"Failed to fetch v1beta discovery: $e1")
            case (_, Left(e2)) => fail(s"Failed to fetch v1 discovery: $e2")
          }
        }
      case None      => IO(println("No API key, skipping API version audit"))
    }
  }

  test("Service Contract Audit") {
    // 1. Reflect on Capability Traits to get Scala methods
    val traits = List(
      classOf[GeminiCore[?]],
      classOf[GeminiFiles[?]],
      classOf[GeminiCaching[?]],
      classOf[GeminiBatch[?]]
    )

    val serviceMethods = traits.flatMap(_.getDeclaredMethods.map(_.getName)).toSet

    // 2. Define Contract: Scala Name -> API Resource.Method
    val contract = Map(
      "generateContent"       -> "models.generateContent",
      "generateContentStream" -> "models.streamGenerateContent",
      "countTokens"           -> "models.countTokens",
      "embedContent"          -> "models.embedContent",
      "batchEmbedContents"    -> "models.batchEmbedContents",
      "uploadFile"            -> "media.upload",
      "listFiles"             -> "files.list",
      "getFile"               -> "files.get",
      "deleteFile"            -> "files.delete",
      "createCachedContent"   -> "cachedContents.create",
      "batchGenerateContent"  -> "models.batchGenerateContent",
      "getBatchJob"           -> "batches.get",
      "listBatchJobs"         -> "batches.list",
      "cancelBatchJob"        -> "batches.cancel",
      "deleteBatchJob"        -> "batches.delete"
    )

    val knownContract = contract

    // 3. Verify all Service methods match contract or are ignored
    val ignoredServiceMethods = Set("equals", "hashCode", "toString", "wait", "notify", "notifyAll", "getClass")
    val untracked             = serviceMethods
      .filterNot(m => m.contains("$") || m.contains("<init>"))
      .diff(contract.keySet)
      .diff(ignoredServiceMethods)

    if (untracked.nonEmpty) {
      fail(s"Service has untracked methods (add to contract): ${untracked.mkString(", ")}")
    }

    // 4. Validate Contract against API
    apiKey match {
      case Some(key) => HttpClientFs2Backend.resource[IO]().use { backend =>
          val httpClient = GeminiHttpClient.make[IO](
            backend,
            ApiKey.unsafe(key),
            baseUrl = "https://generativelanguage.googleapis.com"
          )

          for {
            discovery <- httpClient.get[Json]("$discovery/rest", Map("version" -> "v1beta")).flatMap {
                           case Right(json) => IO.pure(json)
                           case Left(err)   => IO.raiseError(new Exception(s"Discovery fetch failed: $err"))
                         }
          } yield {
            // Flatten API methods: "resource.method"
            def getMethods(json: Json, resourcePrefix: String): Set[String] = {
              val methods         = json.hcursor.downField("methods").keys.getOrElse(Iterable.empty).toSet
              val nestedResources = json.hcursor.downField("resources").keys.getOrElse(Iterable.empty).toSet

              val currentMethods = methods.map(m => s"$resourcePrefix$m")

              val nestedMethods = nestedResources.flatMap { res =>
                val subJson = json.hcursor.downField("resources").downField(res).focus.get
                getMethods(subJson, s"$resourcePrefix$res.")
              }

              currentMethods ++ nestedMethods
            }

            val apiMethods = getMethods(discovery, "")

            println(s"\n=== SERVICE CONTRACT AUDIT ===")
            println(s"Verified ${knownContract.size} methods against API.")

            val missingInApi = knownContract.values.toSet.diff(apiMethods)

            // Print batch related methods to find the real ones
            val batchMethods = apiMethods.filter(m => m.contains("batch") || m.contains("Batch"))
            println(s"Found Batch-related API methods: ${batchMethods.mkString(", ")}")

            if (missingInApi.nonEmpty) {
              fail(s"Contract expects API methods that don't exist: ${missingInApi.mkString(", ")}")
            }

            println("\nService Contract Verified: All mapped methods exist in API.")
          }
        }
      case None      => IO(println("Skipping Service Contract Audit (No API Key)"))
    }
  }
}
