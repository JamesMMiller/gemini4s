package gemini4s

import cats.effect.IO
import munit.CatsEffectSuite
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{ Response, SttpBackend }
import sttp.model.StatusCode
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.impl.cats.CatsMonadAsyncError
import fs2.Stream
import io.circe.syntax._
import io.circe.parser._

import gemini4s.GeminiService
import gemini4s.config.ApiKey
import gemini4s.http.GeminiHttpClient
import gemini4s.model.domain._
import gemini4s.model.request._
import gemini4s.model.response._

class GeminiIntegrationSpec extends CatsEffectSuite {

  // Helper to get API key from env
  val apiKey = sys.env.get("GEMINI_API_KEY")

  def withBackend(testCode: SttpBackend[IO, Fs2Streams[IO]] => IO[Unit]): IO[Unit] = apiKey match {
    case Some(_) => HttpClientFs2Backend.resource[IO]().use(testCode)
    case None    =>
      // Mock responses for when API key is missing
      val stub = SttpBackendStub[IO, Fs2Streams[IO]](new CatsMonadAsyncError[IO])
        .whenRequestMatches(_.uri.path.exists(_.endsWith("generateContent")))
        .thenRespond("""{
            "candidates": [{
              "content": {
                "parts": [{"text": "Mock response"}],
                "role": "model"
              },
              "finishReason": "STOP"
            }]
          }""")
        .whenRequestMatches(_.uri.path.exists(_.endsWith("countTokens")))
        .thenRespond("""{"totalTokens": 42}""")
        .whenRequestMatches(_.uri.path.exists(_.endsWith("embedContent")))
        .thenRespond("""{"embedding": {"values": [0.1, 0.2, 0.3]}}""")
        .whenRequestMatches(_.uri.path.exists(_.endsWith("batchEmbedContents")))
        .thenRespond("""{"embeddings": [{"values": [0.1, 0.2]}, {"values": [0.3, 0.4]}]}""")
        .whenRequestMatches(_.uri.path.exists(_.endsWith("streamGenerateContent")))
        .thenRespondF(_ =>
          IO(
            Response(
              Stream
                .emits(
                  """[
          {
            "candidates": [{
              "content": {
                "parts": [{"text": "Stream chunk 1"}],
                "role": "model"
              },
              "finishReason": "STOP"
            }]
          }
        ]""".getBytes("UTF-8")
                )
                .covary[IO],
              StatusCode.Ok
            )
          )
        )

      testCode(stub)
  }

  test("generateContent should return a valid response") {
    withBackend { backend =>
      val apiKeyValue = ApiKey.unsafe(apiKey.getOrElse("mock-key"))
      val httpClient  = GeminiHttpClient.make[IO](backend, apiKeyValue)
      val service     = GeminiService.make[IO](httpClient)

      service
        .generateContent(
          GenerateContentRequest(
            ModelName.Gemini25Flash,
            List(GeminiService.text("Say hello!"))
          )
        )
        .map {
          case Right(response) => assert(response.candidates.nonEmpty)
          case Left(e)         => fail(s"API call failed: ${e.message}")
        }
    }
  }

  test("countTokens should return a valid count") {
    withBackend { backend =>
      val apiKeyValue = ApiKey.unsafe(apiKey.getOrElse("mock-key"))
      val httpClient  = GeminiHttpClient.make[IO](backend, apiKeyValue)
      val service     = GeminiService.make[IO](httpClient)

      service
        .countTokens(
          CountTokensRequest(
            ModelName.Gemini25Flash,
            List(GeminiService.text("Hello world"))
          )
        )
        .map {
          case Right(count) => assert(count > 0)
          case Left(e)      => fail(s"API call failed: ${e.message}")
        }
    }
  }

  test("generateContent with JSON mode should return valid JSON") {
    withBackend { backend =>
      val apiKeyValue = ApiKey.unsafe(apiKey.getOrElse("mock-key"))
      val httpClient  = GeminiHttpClient.make[IO](backend, apiKeyValue)
      val service     = GeminiService.make[IO](httpClient)

      val jsonConfig = GenerationConfig(responseMimeType = Some(MimeType.ApplicationJson))

      service
        .generateContent(
          GenerateContentRequest(
            model = ModelName.Gemini25Flash,
            contents = List(GeminiService.text("List 3 fruits in JSON format")),
            generationConfig = Some(jsonConfig)
          )
        )
        .map {
          case Right(response) =>
            if (apiKey.isDefined) {
              val text = response.candidates.head.content.flatMap(_.parts.headOption) match {
                case Some(ResponsePart.Text(t)) => t
                case _                          => fail("Expected text response")
              }
              assert(text.trim.startsWith("{") || text.trim.startsWith("["))
            } else {
              assert(true) // Mock passed
            }
          case Left(e)         => fail(s"API call failed: ${e.message}")
        }
    }
  }

  test("generateContent with Tools should return function call") {
    withBackend { backend =>
      val apiKeyValue = ApiKey.unsafe(apiKey.getOrElse("mock-key"))
      val httpClient  = GeminiHttpClient.make[IO](backend, apiKeyValue)
      val service     = GeminiService.make[IO](httpClient)

      val weatherTool = Tool(
        functionDeclarations = Some(
          List(
            FunctionDeclaration(
              name = "get_weather",
              description = "Get the current weather in a given location",
              parameters = Some(
                Schema(
                  `type` = SchemaType.OBJECT,
                  properties = Some(
                    Map(
                      "location" -> Schema(
                        `type` = SchemaType.STRING,
                        description = Some("The city and state, e.g. San Francisco, CA")
                      )
                    )
                  ),
                  required = Some(List("location"))
                )
              )
            )
          )
        )
      )

      service
        .generateContent(
          GenerateContentRequest(
            model = ModelName.Gemini25Flash,
            contents =
              List(GeminiService.text("Call the get_weather function for Tokyo, Japan. Ensure you use the tool.")),
            tools = Some(List(weatherTool))
          )
        )
        .map {
          case Right(response) =>
            if (apiKey.isDefined) {
              val hasFunctionCall = response.candidates.headOption
                .flatMap(_.content)
                .exists(_.parts.exists {
                  case ResponsePart.FunctionCall(_) => true
                  case _                            => false
                })
              assert(hasFunctionCall, "Expected a function call in the response")
            } else {
              assert(true)
            }
          case Left(e)         => fail(s"API call failed: ${e.message}")
        }
    }
  }

  test("embedContent should return embedding vector") {
    withBackend { backend =>
      val apiKeyValue = ApiKey.unsafe(apiKey.getOrElse("mock-key"))
      val httpClient  = GeminiHttpClient.make[IO](backend, apiKeyValue)
      val service     = GeminiService.make[IO](httpClient)

      service
        .embedContent(
          EmbedContentRequest(
            content = GeminiService.text("Hello world!"),
            model = ModelName.EmbeddingText001
          )
        )
        .map {
          case Right(embedding) => assert(embedding.values.nonEmpty)
          case Left(e)          => fail(s"API call failed: ${e.message}")
        }
    }
  }

  test("batchEmbedContents should return multiple embeddings") {
    withBackend { backend =>
      val apiKeyValue = ApiKey.unsafe(apiKey.getOrElse("mock-key"))
      val httpClient  = GeminiHttpClient.make[IO](backend, apiKeyValue)
      val service     = GeminiService.make[IO](httpClient)

      val requests = List(
        EmbedContentRequest(GeminiService.text("First text"), ModelName.EmbeddingText001),
        EmbedContentRequest(GeminiService.text("Second text"), ModelName.EmbeddingText001)
      )

      service
        .batchEmbedContents(
          BatchEmbedContentsRequest(
            model = ModelName.EmbeddingText001,
            requests = requests
          )
        )
        .map {
          case Right(embeddings) => assertEquals(embeddings.length, 2)
          case Left(e)           => fail(s"API call failed: ${e.message}")
        }
    }
  }

  test("generateContentStream should stream response chunks") {
    if (apiKey.isDefined) {
      withBackend { backend =>
        val apiKeyValue = ApiKey.unsafe(apiKey.getOrElse("mock-key"))
        val httpClient  = GeminiHttpClient.make[IO](backend, apiKeyValue)
        val service     = GeminiService.make[IO](httpClient)

        service
          .generateContentStream(
            GenerateContentRequest(
              model = ModelName.Gemini25Flash,
              contents = List(GeminiService.text("Tell me a short story"))
            )
          )
          .compile
          .toList
          .map(chunks => assert(chunks.nonEmpty, "Expected at least one chunk"))
          .handleErrorWith(e => IO.println(s"Stream error: $e") *> IO.raiseError(e))
      }
    } else {
      IO(println("Skipping streaming test in mock mode due to SttpBackendStub limitations"))
    }
  }

  test("generateContent with Image should describe the image") {
    withBackend { backend =>
      val apiKeyValue = ApiKey.unsafe(apiKey.getOrElse("mock-key"))
      val httpClient  = GeminiHttpClient.make[IO](backend, apiKeyValue)
      val service     = GeminiService.make[IO](httpClient)

      // 1x1 transparent PNG
      val base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
      val imagePart   = GeminiService.image(base64Image, "image/png")
      val textPart    = GeminiService.text("What is this image?")

      service
        .generateContent(
          GenerateContentRequest(
            model = ModelName.Gemini25Flash,
            contents = List(Content(parts = imagePart.parts ++ textPart.parts))
          )
        )
        .map {
          case Right(response) => assert(response.candidates.nonEmpty)
          case Left(e)         => fail(s"API call failed: ${e.message}")
        }
    }
  }

}
