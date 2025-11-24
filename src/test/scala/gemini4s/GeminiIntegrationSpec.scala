package gemini4s

import cats.effect.IO
import munit.CatsEffectSuite
import sttp.client3.httpclient.fs2.HttpClientFs2Backend

import gemini4s.config.GeminiConfig
import gemini4s.http.GeminiHttpClient
import gemini4s.interpreter.GeminiServiceImpl
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
      val config     = GeminiConfig(apiKey.getOrElse(""))
      val httpClient = GeminiHttpClient.make[IO](backend, config)
      val service    = GeminiServiceImpl.make[IO](httpClient)

      service
        .generateContent(
          GenerateContentRequest("gemini-2.0-flash-lite-preview-02-05", List(GeminiService.text("Say hello!")))
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
      val config     = GeminiConfig(apiKey.getOrElse(""))
      val httpClient = GeminiHttpClient.make[IO](backend, config)
      val service    = GeminiServiceImpl.make[IO](httpClient)

      service
        .countTokens(
          CountTokensRequest("gemini-2.0-flash-lite-preview-02-05", List(GeminiService.text("Hello world")))
        )
        .map {
          case Right(count) => assert(count > 0)
          case Left(e)      =>
            println(s"API Error: ${e.message}")
            fail(s"API call failed: ${e.message}")
        }
    }
  }

  test("generateContent with JSON mode should return valid JSON") {
    HttpClientFs2Backend.resource[IO]().use { backend =>
      val config     = GeminiConfig(apiKey.getOrElse(""))
      val httpClient = GeminiHttpClient.make[IO](backend, config)
      val service    = GeminiServiceImpl.make[IO](httpClient)

      val jsonConfig = GenerationConfig(responseMimeType = Some("application/json"))

      service
        .generateContent(
          GenerateContentRequest(
            model = "gemini-2.0-flash-lite-preview-02-05",
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

  test("generateContent with Tools should return function call") {
    HttpClientFs2Backend.resource[IO]().use { backend =>
      val config     = GeminiConfig(apiKey.getOrElse(""))
      val httpClient = GeminiHttpClient.make[IO](backend, config)
      val service    = GeminiServiceImpl.make[IO](httpClient)

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
            model = "gemini-2.0-flash-lite-preview-02-05",
            contents = List(GeminiService.text("What is the weather in Chicago, IL?")),
            tools = Some(List(weatherTool)),
            toolConfig = Some(
              ToolConfig(functionCallingConfig = Some(FunctionCallingConfig(mode = Some(FunctionCallingMode.AUTO))))
            )
          )
        )
        .map {
          case Right(response) =>
            val part = response.candidates.head.content.parts.head
            part match {
              case ResponsePart.FunctionCall(data) =>
                assertEquals(data.name, "get_weather")
                assert(data.args.contains("location"))
              case _                               => fail(s"Expected FunctionCall, got $part")
            }
          case Left(e)         => fail(s"API call failed: ${e.message}")
        }
    }
  }

  test("embedContent should return embeddings") {
    HttpClientFs2Backend.resource[IO]().use { backend =>
      val config     = GeminiConfig(apiKey.getOrElse(""))
      val httpClient = GeminiHttpClient.make[IO](backend, config)
      val service    = GeminiServiceImpl.make[IO](httpClient)

      service
        .embedContent(
          EmbedContentRequest(GeminiService.text("Hello world"), "models/text-embedding-004")
        )
        .map {
          case Right(embedding) => assert(embedding.values.nonEmpty)
          case Left(e)          => fail(s"API call failed: ${e.message}")
        }
    }
  }

  test("batchEmbedContents should return multiple embeddings") {
    HttpClientFs2Backend.resource[IO]().use { backend =>
      val config     = GeminiConfig(apiKey.getOrElse(""))
      val httpClient = GeminiHttpClient.make[IO](backend, config)
      val service    = GeminiServiceImpl.make[IO](httpClient)

      val model = GeminiConstants.EmbeddingText004

      service
        .batchEmbedContents(
          BatchEmbedContentsRequest(
            model = model,
            requests = List(
              EmbedContentRequest(content = GeminiService.text("Hello"), model = model),
              EmbedContentRequest(content = GeminiService.text("World"), model = model)
            )
          )
        )
        .map {
          case Right(embeddings) =>
            assertEquals(embeddings.length, 2)
            assert(embeddings.forall(_.values.nonEmpty))
          case Left(e)           => fail(s"API call failed: ${e.message}")
        }
    }
  }

  test("generateContentStream should return a stream of responses") {
    HttpClientFs2Backend.resource[IO]().use { backend =>
      val config     = GeminiConfig(apiKey.getOrElse(""))
      val httpClient = GeminiHttpClient.make[IO](backend, config)
      val service    = GeminiServiceImpl.make[IO](httpClient)

      service
        .generateContentStream(
          GenerateContentRequest(
            "gemini-2.0-flash-lite-preview-02-05",
            List(GeminiService.text("Count from 1 to 5 slowly"))
          )
        )
        .compile
        .toList
        .map { responses =>
          assert(responses.nonEmpty)
          val fullText = responses
            .flatMap(_.candidates.headOption.flatMap(_.content.parts.headOption).map {
              case ResponsePart.Text(t) => t
              case _                    => ""
            })
            .mkString
          assert(fullText.nonEmpty)
        }
    }
  }
}
