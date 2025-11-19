package gemini4s

import cats.effect.IO
import munit.CatsEffectSuite
import sttp.client3.httpclient.fs2.HttpClientFs2Backend

import gemini4s.config.GeminiConfig
import gemini4s.http.GeminiHttpClient
import gemini4s.interpreter.GeminiServiceImpl
import gemini4s.model.GeminiRequest._

class GeminiIntegrationSpec extends CatsEffectSuite {

  // Helper to get API key from env
  val apiKey = sys.env.get("GEMINI_API_KEY")

  // Skip tests if no API key is present
  override def munitIgnore: Boolean = apiKey.isEmpty

  test("generateContent should return a valid response") {
    HttpClientFs2Backend.resource[IO]().use { backend =>
      val httpClient                    = GeminiHttpClient.make[IO](backend)
      val service                       = GeminiServiceImpl.make[IO](httpClient)
      implicit val config: GeminiConfig = GeminiConfig(apiKey.getOrElse(""))

      service
        .generateContent(
          contents = List(GeminiService.text("Say hello!"))
        )
        .map {
          case Right(_) => assert(true)
          case Left(e)  =>
            println(s"API Error: ${e.message}")
            e.cause.foreach(_.printStackTrace())
            fail(s"API call failed: ${e.message}")
        }
    }
  }

  test("countTokens should return a valid count") {
    HttpClientFs2Backend.resource[IO]().use { backend =>
      val httpClient                    = GeminiHttpClient.make[IO](backend)
      val service                       = GeminiServiceImpl.make[IO](httpClient)
      implicit val config: GeminiConfig = GeminiConfig(apiKey.getOrElse(""))

      service
        .countTokens(
          List(GeminiService.text("Hello world"))
        )
        .map {
          case Right(count) => assert(count > 0)
          case Left(e)      =>
            println(s"API Error: ${e.message}")
            fail(s"API call failed: ${e.message}")
        }
    }
  }
}
