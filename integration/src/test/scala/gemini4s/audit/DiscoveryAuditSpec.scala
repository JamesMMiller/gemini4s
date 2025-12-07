package gemini4s.audit

import cats.effect.IO
import munit.CatsEffectSuite
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import gemini4s.config.ApiKey
import gemini4s.http.GeminiHttpClient
import io.circe.Json
import io.circe.parser._
import scala.io.Source
import java.io.File

class DiscoveryAuditSpec extends CatsEffectSuite {

  // Define structure for config
  case class IgnoreItem(name: String, reason: String, link: String)
  case class ResourceConfig(implemented: List[String], ignored: List[IgnoreItem])
  case class Config(resources: Map[String, ResourceConfig], schemas: Map[String, ResourceConfig])

  // Simple manual decoder to avoid boilerplate if generic extras aren't available
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
                // We only check resources we explicitly track in config, or warn if new resource appears
                if (!config.resources.contains(resName)) {
                  // For now, checking 'models'. Can create warning for unknown resources later.
                  // errors :+= s"New Resource Discovered: $resName"
                } else {
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

              // Special case for 'models' if it's top level? Discovery usually nests.
              // Assuming standard Google Discovery format where methods can be on resources.

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
                  s"Compliance Audit Failed:\n${errors.mkString("\n")}\n\nPlease update audit/compliance_config.json with 'ignored' entries if these are known missing features."
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
}
