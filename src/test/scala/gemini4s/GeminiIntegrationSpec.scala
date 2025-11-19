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
          contents = List(GeminiService.text("Say hello!")),
          safetySettings = None,
          generationConfig = None,
          systemInstruction = None
        )
        .map {
          case Right(response) =>
            assert(response.candidates.nonEmpty)
            assert(response.candidates.head.content.parts.nonEmpty)
          case Left(error)     => fail(s"API call failed: ${error.message}")
        }
    }
  }

  test("generateContent with systemInstruction should behave correctly".ignore) {
    HttpClientFs2Backend.resource[IO]().use { backend =>
      val httpClient                    = GeminiHttpClient.make[IO](backend)
      val service                       = GeminiServiceImpl.make[IO](httpClient)
      implicit val config: GeminiConfig = GeminiConfig(apiKey.getOrElse(""))

      service
        .generateContent(
          contents = List(GeminiService.text("Who are you?")),
          safetySettings = None,
          generationConfig = None,
          systemInstruction = Some(GeminiService.text("You are a helpful assistant named Gemini4s."))
        )
        .map {
          case Right(response) =>
            assert(response.candidates.nonEmpty)
            val text = response.candidates.head.content.parts.map(_.text).mkString
            assert(text.contains("Gemini4s"))
          case Left(error)     => fail(s"API call failed: ${error.message}")
        }
    }
  }

  test("generateContent with inlineData (image) should work".ignore) {
    // 1x1 transparent PNG pixel
    val base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
    HttpClientFs2Backend.resource[IO]().use { backend =>
      val httpClient                    = GeminiHttpClient.make[IO](backend)
      val service                       = GeminiServiceImpl.make[IO](httpClient)
      implicit val config: GeminiConfig = GeminiConfig(apiKey.getOrElse(""))

      service
        .generateContent(
          contents = List(
            GeminiService.text("What is this image?"),
            GeminiService.image("image/png", base64Image)
          ),
          safetySettings = None,
          generationConfig = None,
          systemInstruction = None
        )
        .map {
          case Right(response) => assert(response.candidates.nonEmpty)
          case Left(error)     => fail(s"API call failed: ${error.message}")
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
          contents = List(GeminiService.text("Hello world"))
        )
        .map {
          case Right(response) => assert(response.totalTokens > 0)
          case Left(error)     => fail(s"API call failed: ${error.message}")
        }
    }
  }
}
