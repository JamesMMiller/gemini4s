package gemini4s.audit

import cats.effect.IO
import munit.CatsEffectSuite
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import gemini4s.config.ApiKey
import gemini4s.http.GeminiHttpClient
import gemini4s.model.domain.ModelCapabilities._
import io.circe.Json
import io.circe.parser._
import scala.io.Source
import java.io.File

class DiscoveryAuditSpec extends CatsEffectSuite {

  // Define structure for config
  case class IgnoreItem(name: String, reason: String, link: String)
  case class ResourceConfig(implemented: List[String], ignored: List[IgnoreItem])
  case class Config(resources: Map[String, ResourceConfig], schemas: Map[String, ResourceConfig])

  // Model capability assertions - maps model name patterns to expected capabilities
  // NOTE: streamGenerateContent is NOT listed in supportedGenerationMethods but IS available
  // When generateContent is supported, streaming is implicitly available via the :streamGenerateContent endpoint
  case class ModelCapabilityAssertion(
      modelPattern: String,
      expectedMethods: Set[String],
      description: String
  )

  // Our capability model assertions - based on what the API actually reports
  // Note: streaming is always available when generateContent is supported
  val capabilityAssertions = List(
    // Generation models - core capabilities
    ModelCapabilityAssertion(
      "gemini-2.5-flash",
      Set("generateContent", "countTokens"), // batchGenerateContent varies by variant
      "Gemini 2.5 Flash - Core Generation"
    ),
    ModelCapabilityAssertion(
      "gemini-2.5-pro",
      Set("generateContent", "countTokens"),
      "Gemini 2.5 Pro - Core Generation"
    ),
    // Embedding models should support embedContent
    ModelCapabilityAssertion(
      "gemini-embedding-001",
      Set("embedContent", "countTokens"),
      "Gemini Embedding 001 - EmbeddingCapabilities"
    ),
    ModelCapabilityAssertion(
      "text-embedding",
      Set("embedContent"),
      "Text Embedding models - EmbeddingCapabilities"
    )
  )

  import io.circe._
  implicit val ignoreItemDecoder: Decoder[IgnoreItem] = Decoder.forProduct3("name", "reason", "link")(IgnoreItem.apply)

  implicit val resourceConfigDecoder: Decoder[ResourceConfig] =
    Decoder.forProduct2("implemented", "ignored")(ResourceConfig.apply)

  implicit val configDecoder: Decoder[Config] = Decoder.forProduct2("resources", "schemas")(Config.apply)

  val apiKey = sys.env.get("GEMINI_API_KEY")

  test("Automated API Compliance Audit") {
    println(s"DEBUG: CWD is ${System.getProperty("user.dir")}")
    val configFile = new File("src/test/resources/compliance_config.json")

    if (!configFile.exists()) {
      fail(s"Config file not found at: ${configFile.getAbsolutePath}")
    }

    val configJson = Source.fromFile(configFile).mkString
    val config     = decode[Config](configJson).fold(
      err => throw new Exception(s"JSON Decode Error: $err"),
      c => c
    )
    println("DEBUG: Config loaded successfully")

    apiKey match {
      case Some(key) => HttpClientFs2Backend.resource[IO]().use { backend =>
          val httpClient = GeminiHttpClient.make[IO](
            backend,
            ApiKey.unsafe(key),
            baseUrl = "https://generativelanguage.googleapis.com"
          )

          httpClient.get[Json]("$discovery/rest", Map("version" -> "v1beta")).map {
            case Right(discovery) =>
              var errors = List.empty[String]

              // --- Audit Resources & Methods ---
              val resources = discovery.hcursor.downField("resources").keys.getOrElse(Iterable.empty)

              resources.foreach { resName =>
                if (config.resources.contains(resName)) {
                  val methodsCursor = discovery.hcursor.downField("resources").downField(resName).downField("methods")
                  val apiMethods    = methodsCursor.keys.getOrElse(Iterable.empty).toList

                  val conf    = config.resources(resName)
                  val tracked = conf.implemented ++ conf.ignored.map(_.name)

                  val missing = apiMethods.diff(tracked)
                  if (missing.nonEmpty) {
                    errors :+= s"Resource '$resName' has unimplemented/untracked methods: ${missing.mkString(", ")}"
                  }
                }
              }

              // --- Audit Schemas (GenerationConfig) ---
              val genConfigProps =
                discovery.hcursor.downField("schemas").downField("GenerationConfig").downField("properties")
              val apiProps       = genConfigProps.keys.getOrElse(Iterable.empty).toList

              val confSchema   = config.schemas("GenerationConfig")
              val trackedProps = confSchema.implemented ++ confSchema.ignored.map(_.name)

              val missingProps = apiProps.diff(trackedProps)
              if (missingProps.nonEmpty) {
                errors :+= s"Schema 'GenerationConfig' has untracked fields: ${missingProps.mkString(", ")}"
              }

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
                  name.contains(assertion.modelPattern)
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
}
