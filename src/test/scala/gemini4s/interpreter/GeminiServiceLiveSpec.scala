package gemini4s.interpreter

import zio._
import zio.json.JsonDecoder
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test._

import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError
import gemini4s.http.GeminiHttpClient
import gemini4s.model.GeminiRequest._
import gemini4s.model.GeminiResponse._
import gemini4s.model.{GeminiRequest, GeminiResponse}

object GeminiServiceLiveSpec extends ZIOSpecDefault {
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
              parts = List(ResponsePart(text = s"Response for: ${request.asInstanceOf[GenerateContent].contents.head.parts.head.text}")),
              role = Some("model")
            ),
            finishReason = Some("STOP"),
            safetyRatings = None
          )
        ),
        usageMetadata = None,
        modelVersion = None
      )
      ZIO.succeed(Right(response.asInstanceOf[Res]))
    }

    override def postStream[Req <: GeminiRequest, Res: JsonDecoder](
      endpoint: String,
      request: Req
    )(using config: GeminiConfig): Task[ZStream[Any, GeminiError, Res]] = {
      // Simulate successful streaming response
      val response = GenerateContentResponse(
        candidates = List(
          Candidate(
            content = ResponseContent(
              parts = List(ResponsePart(text = s"Streaming response for: ${request.asInstanceOf[GenerateContent].contents.head.parts.head.text}")),
              role = Some("model")
            ),
            finishReason = Some("STOP"),
            safetyRatings = None
          )
        ),
        usageMetadata = None,
        modelVersion = None
      )
      ZIO.succeed(ZStream.succeed(response.asInstanceOf[Res]))
    }
  }

  def spec = suite("GeminiServiceLive")(
    test("generateContent should return expected response") {
      val service = GeminiServiceLive(new TestHttpClient)
      val input = "Test input"
      val config = GeminiConfig("test-api-key")

      for {
        result <- service.generateContent(List(Content(parts = List(Part(text = input)))))(using config)
      } yield assertTrue(
        result.isRight,
        result.toOption.get.candidates.head.content.parts.head == ResponsePart(text = "Response for: Test input")
      )
    },

    test("generateContentStream should return expected response") {
      val service = GeminiServiceLive(new TestHttpClient)
      val input = "Test input"
      val config = GeminiConfig("test-api-key")

      for {
        stream <- service.generateContentStream(List(Content(parts = List(Part(text = input)))))(using config)
        result <- stream.runHead
      } yield assertTrue(
        result.isDefined,
        result.get.candidates.head.content.parts.head == ResponsePart(text = "Streaming response for: Test input")
      )
    }
  )
} 