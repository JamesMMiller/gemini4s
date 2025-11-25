package gemini4s

import cats.effect.IO
import munit.CatsEffectSuite
import sttp.client3.httpclient.fs2.HttpClientFs2Backend

import gemini4s.config.ApiKey
import gemini4s.http.GeminiHttpClient
import gemini4s.impl.GeminiServiceImpl
import gemini4s.model.domain._
import gemini4s.model.request._
import gemini4s.model.response._

class GeminiIntegrationSpec extends CatsEffectSuite {

  // Helper to get API key from env
  val apiKey = sys.env.get("GEMINI_API_KEY")

  // Skip tests if no API key is present
  override def munitIgnore: Boolean = apiKey.isEmpty

  test("generateContent should return a valid response") {
    HttpClientFs2Backend.resource[IO]().use { backend =>
      val apiKeyValue = ApiKey.unsafe(apiKey.getOrElse(""))
      GeminiHttpClient.make[IO](backend, apiKeyValue).use { httpClient =>
        val service = GeminiServiceImpl.make[IO](httpClient)

        service
          .generateContent(
            GenerateContentRequest(
              ModelName.Gemini25Flash,
              List(GeminiService.text("Say hello!"))
            )
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
  }

  test("countTokens should return a valid count") {
    HttpClientFs2Backend.resource[IO]().use { backend =>
      val apiKeyValue = ApiKey.unsafe(apiKey.getOrElse(""))
      GeminiHttpClient.make[IO](backend, apiKeyValue).use { httpClient =>
        val service = GeminiServiceImpl.make[IO](httpClient)

        service
          .countTokens(
            CountTokensRequest(
              ModelName.Gemini25Flash,
              List(GeminiService.text("Hello world"))
            )
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

  test("generateContent with JSON mode should return valid JSON") {
    HttpClientFs2Backend.resource[IO]().use { backend =>
      val apiKeyValue = ApiKey.unsafe(apiKey.getOrElse(""))
      GeminiHttpClient.make[IO](backend, apiKeyValue).use { httpClient =>
        val service = GeminiServiceImpl.make[IO](httpClient)

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
              val text = response.candidates.head.content.parts.head match {
                case ResponsePart.Text(t) => t
                case _                    => fail("Expected text response")
              }
              assert(text.trim.startsWith("{") || text.trim.startsWith("["))
            case Left(e)         => fail(s"API call failed: ${e.message}")
          }
      }
    }
  }

  test("generateContent with Tools should return function call") {
    HttpClientFs2Backend.resource[IO]().use { backend =>
      val apiKeyValue = ApiKey.unsafe(apiKey.getOrElse(""))
      GeminiHttpClient.make[IO](backend, apiKeyValue).use { httpClient =>
        val service = GeminiServiceImpl.make[IO](httpClient)

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
              contents = List(GeminiService.text("What's the weather in Tokyo? Please use the get_weather tool.")),
              tools = Some(List(weatherTool))
            )
          )
          .map {
            case Right(response) =>
              val hasFunctionCall = response.candidates.headOption.exists(_.content.parts.exists {
                case ResponsePart.FunctionCall(_) => true
                case _                            => false
              })
              assert(hasFunctionCall, "Expected a function call in the response")
            case Left(e)         => fail(s"API call failed: ${e.message}")
          }
      }
    }
  }

  test("embedContent should return embedding vector") {
    HttpClientFs2Backend.resource[IO]().use { backend =>
      val apiKeyValue = ApiKey.unsafe(apiKey.getOrElse(""))
      GeminiHttpClient.make[IO](backend, apiKeyValue).use { httpClient =>
        val service = GeminiServiceImpl.make[IO](httpClient)

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
  }

  test("batchEmbedContents should return multiple embeddings") {
    HttpClientFs2Backend.resource[IO]().use { backend =>
      val apiKeyValue = ApiKey.unsafe(apiKey.getOrElse(""))
      GeminiHttpClient.make[IO](backend, apiKeyValue).use { httpClient =>
        val service = GeminiServiceImpl.make[IO](httpClient)

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
  }

  test("generateContentStream should stream response chunks") {
    HttpClientFs2Backend.resource[IO]().use { backend =>
      val apiKeyValue = ApiKey.unsafe(apiKey.getOrElse(""))
      GeminiHttpClient.make[IO](backend, apiKeyValue).use { httpClient =>
        val service = GeminiServiceImpl.make[IO](httpClient)

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
      }
    }
  }

}
