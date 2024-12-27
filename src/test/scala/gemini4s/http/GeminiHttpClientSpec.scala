package gemini4s.http

import zio._
import zio.json._
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
    )(using config: GeminiConfig): Task[Either[GeminiError, Res]] = endpoint match {
      case "success" =>
        val response = GenerateContentResponse(
          candidates = List(
            Candidate(
              content = ResponseContent(
                parts = List(ResponsePart(text = "Generated text")),
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

      case "network-error" =>
        ZIO.succeed(Left(GeminiError.ConnectionError("Connection failed", Some(new RuntimeException("Network error")))))

      case "api-error" =>
        ZIO.succeed(Left(GeminiError.InvalidRequest("API error: 400 - Invalid request", None)))

      case "invalid-response" =>
        ZIO.succeed(Right("Invalid response format".asInstanceOf[Res]))

      case _ =>
        ZIO.succeed(Left(GeminiError.InvalidRequest("Unknown endpoint", None)))
    }

    override def postStream[Req <: GeminiRequest, Res: JsonDecoder](
      endpoint: String,
      request: Req
    )(using config: GeminiConfig): Task[ZStream[Any, GeminiError, Res]] = endpoint match {
      case "success" =>
        val responses = List(
          GenerateContentResponse(
            candidates = List(
              Candidate(
                content = ResponseContent(
                  parts = List(ResponsePart(text = "First")),
                  role = Some("model")
                ),
                finishReason = Some("STOP"),
                index = None,
                safetyRatings = None
              )
            ),
            usageMetadata = None,
            modelVersion = None
          ),
          GenerateContentResponse(
            candidates = List(
              Candidate(
                content = ResponseContent(
                  parts = List(ResponsePart(text = "Second")),
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

      case "stream-error" =>
        ZIO.succeed(ZStream.fail(GeminiError.ConnectionError("Stream error", None)))

      case "stream-interrupt" =>
        ZIO.succeed(ZStream.from(ZIO.interrupt))

      case _ =>
        ZIO.succeed(ZStream.fail(GeminiError.InvalidRequest("Unknown endpoint", None)))
    }
  }

  def spec = suite("GeminiHttpClient")(
    test("post should return successful response") {
      val client = new TestHttpClient()
      given config: GeminiConfig = GeminiConfig("test-api-key")
      val request = GenerateContent(
        contents = List(Content(parts = List(Part(text = "Test input")))),
        safetySettings = None,
        generationConfig = None
      )

      for {
        result <- client.post[GenerateContent, GenerateContentResponse]("success", request)
      } yield assertTrue(
        result.isRight,
        result.exists(_.candidates.nonEmpty),
        result.exists(_.candidates.head.content.parts.head == ResponsePart(text = "Generated text"))
      )
    },

    test("post should handle network errors") {
      val client = new TestHttpClient()
      given config: GeminiConfig = GeminiConfig("test-api-key")
      val request = GenerateContent(
        contents = List(Content(parts = List(Part(text = "Test input")))),
        safetySettings = None,
        generationConfig = None
      )

      for {
        result <- client.post[GenerateContent, GenerateContentResponse]("network-error", request)
      } yield assertTrue(
        result.isLeft,
        result.left.exists(_.isInstanceOf[GeminiError.ConnectionError]),
        result.left.exists(_.message == "Connection failed"),
        result.left.exists(_.cause.exists(_.getMessage == "Network error"))
      )
    },

    test("post should handle API errors") {
      val client = new TestHttpClient()
      given config: GeminiConfig = GeminiConfig("test-api-key")
      val request = GenerateContent(
        contents = List(Content(parts = List(Part(text = "Test input")))),
        safetySettings = None,
        generationConfig = None
      )

      for {
        result <- client.post[GenerateContent, GenerateContentResponse]("api-error", request)
      } yield assertTrue(
        result.isLeft,
        result.left.exists(_.isInstanceOf[GeminiError.InvalidRequest]),
        result.left.exists(_.message == "API error: 400 - Invalid request")
      )
    },

    test("postStream should return successful stream") {
      val client = new TestHttpClient()
      given config: GeminiConfig = GeminiConfig("test-api-key")
      val request = GenerateContent(
        contents = List(Content(parts = List(Part(text = "Test input")))),
        safetySettings = None,
        generationConfig = None
      )

      for {
        stream <- client.postStream[GenerateContent, GenerateContentResponse]("success", request)
        result <- stream.runCollect
      } yield assertTrue(
        result.nonEmpty,
        result.head.candidates.head.content.parts.head == ResponsePart(text = "First"),
        result.last.candidates.head.content.parts.head == ResponsePart(text = "Second")
      )
    },

    test("postStream should handle stream errors") {
      val client = new TestHttpClient()
      given config: GeminiConfig = GeminiConfig("test-api-key")
      val request = GenerateContent(
        contents = List(Content(parts = List(Part(text = "Test input")))),
        safetySettings = None,
        generationConfig = None
      )

      for {
        stream <- client.postStream[GenerateContent, GenerateContentResponse]("stream-error", request)
        result <- stream.runCollect.either
      } yield assertTrue(
        result.isLeft,
        result.left.exists(_.isInstanceOf[GeminiError.ConnectionError]),
        result.left.exists(_.message == "Stream error")
      )
    },

    test("postStream should handle stream interruption") {
      val client = new TestHttpClient()
      given config: GeminiConfig = GeminiConfig("test-api-key")
      val request = GenerateContent(
        contents = List(Content(parts = List(Part(text = "Test input")))),
        safetySettings = None,
        generationConfig = None
      )

      for {
        stream <- client.postStream[GenerateContent, GenerateContentResponse]("stream-interrupt", request)
        result <- stream.runCollect.exit
      } yield assertTrue(result.isInterrupted)
    }
  )
} 