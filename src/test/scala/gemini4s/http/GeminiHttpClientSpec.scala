package gemini4s.http

import zio._
import zio.json.JsonDecoder
import zio.stream._
import zio.test.Assertion._
import zio.test._

import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError
import gemini4s.model.GeminiCodecs.given
import gemini4s.model.GeminiRequest
import gemini4s.model.GeminiRequest._
import gemini4s.model.GeminiResponse._

object GeminiHttpClientSpec extends ZIOSpecDefault {
  class TestHttpClient extends GeminiHttpClient[Task] {
    override def post[Req <: GeminiRequest, Res: JsonDecoder](
      endpoint: String,
      request: Req
    )(using config: GeminiConfig): Task[Either[GeminiError, Res]] = {
      // Simulate successful response for test content
      val response = GenerateContentResponse(
        candidates = List(
          Candidate(
            content = ResponseContent(
              parts = List(Part(text = "Generated text")),
              role = Some("model")
            ),
            finishReason = Some("STOP"),
            index = None,
            safetyRatings = Some(List(
              SafetyRating(
                category = "HARASSMENT",
                probability = "LOW"
              )
            ))
          )
        ),
        usageMetadata = Some(
          UsageMetadata(
            promptTokenCount = 10,
            candidatesTokenCount = 20,
            totalTokenCount = 30
          )
        ),
        modelVersion = Some("gemini-pro")
      )
      ZIO.succeed(Right(response.asInstanceOf[Res]))
    }

    override def postStream[Req <: GeminiRequest, Res: JsonDecoder](
      endpoint: String,
      request: Req
    )(using config: GeminiConfig): Task[ZStream[Any, GeminiError, Res]] = {
      // Simulate successful streaming response
      val responses = List(
        GenerateContentResponse(
          candidates = List(
            Candidate(
              content = ResponseContent(
                parts = List(Part(text = "First")),
                role = Some("model")
              ),
              finishReason = Some("STOP"),
              index = None,
              safetyRatings = Some(List(
                SafetyRating(
                  category = "HARASSMENT",
                  probability = "LOW"
                )
              ))
            )
          ),
          usageMetadata = Some(
            UsageMetadata(
              promptTokenCount = 10,
              candidatesTokenCount = 20,
              totalTokenCount = 30
            )
          ),
          modelVersion = Some("gemini-pro")
        ),
        GenerateContentResponse(
          candidates = List(
            Candidate(
              content = ResponseContent(
                parts = List(Part(text = "Second")),
                role = Some("model")
              ),
              finishReason = Some("STOP"),
              index = None,
              safetyRatings = None
            )
          ),
          usageMetadata = None,
          modelVersion = None
        )
      )
      ZIO.succeed(ZStream.fromIterable(responses.asInstanceOf[List[Res]]))
    }
  }

  def spec = suite("GeminiHttpClient")(
    test("post should return successful response") {
      val client = new TestHttpClient()
      given config: GeminiConfig = GeminiConfig("test-api-key")
      val request = GenerateContent(
        contents = List(Content.Text("Test input")),
        safetySettings = None,
        generationConfig = None
      )

      for {
        result <- client.post[GenerateContent, GenerateContentResponse]("test-endpoint", request)
      } yield assertTrue(
        result.isRight,
        result.exists(_.asInstanceOf[GenerateContentResponse].candidates.nonEmpty),
        result.exists(_.asInstanceOf[GenerateContentResponse].candidates.head.content.parts.head == Part(text = "Generated text"))
      )
    },

    test("postStream should return successful stream") {
      val client = new TestHttpClient()
      given config: GeminiConfig = GeminiConfig("test-api-key")
      val request = GenerateContent(
        contents = List(Content.Text("Test input")),
        safetySettings = None,
        generationConfig = None
      )

      for {
        stream <- client.postStream[GenerateContent, GenerateContentResponse]("test-endpoint", request)
        result <- stream.runCollect
      } yield assertTrue(
        result.nonEmpty,
        result.head.asInstanceOf[GenerateContentResponse].candidates.head.content.parts.head == Part(text = "First"),
        result.last.asInstanceOf[GenerateContentResponse].candidates.head.content.parts.head == Part(text = "Second")
      )
    }
  )
} 