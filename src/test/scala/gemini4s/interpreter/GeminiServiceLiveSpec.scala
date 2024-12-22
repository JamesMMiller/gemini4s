package gemini4s.interpreter

import zio._
import zio.json._
import zio.stream.ZStream
import zio.test._
import zio.test.Assertion._

import gemini4s.GeminiService
import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError
import gemini4s.http.GeminiHttpClient
import gemini4s.model.{GeminiRequest, GeminiResponse}
import gemini4s.model.GeminiRequest._
import gemini4s.model.GeminiResponse._

object GeminiServiceLiveSpec extends ZIOSpecDefault {
  // Test HTTP client that returns predefined responses
  class TestHttpClient extends GeminiHttpClient[Task] {
    override def post[Req <: GeminiRequest, Res <: GeminiResponse](
      endpoint: String,
      request: Req
    )(using config: GeminiConfig): Task[Either[GeminiError, Res]] = {
      request match {
        case GenerateContent(contents, _, _) =>
          val response = GenerateContentResponse(
            candidates = List(
              Candidate(
                content = ResponseContent(
                  parts = List(ResponsePart.Text(s"Response for: ${contents.head}")),
                  role = Some("model")
                ),
                finishReason = FinishReason.STOP,
                safetyRatings = List.empty,
                citationMetadata = None
              )
            ),
            promptFeedback = None
          )
          ZIO.succeed(Right(response.asInstanceOf[Res]))

        case CountTokensRequest(contents) =>
          val response = CountTokensResponse(42)
          ZIO.succeed(Right(response.asInstanceOf[Res]))

        case _ =>
          ZIO.succeed(Left(GeminiError.InvalidRequest("Unsupported request type")))
      }
    }

    override def postStream[Req <: GeminiRequest, Res <: GeminiResponse](
      endpoint: String,
      request: Req
    )(using config: GeminiConfig): Task[ZStream[Any, GeminiError, Res]] = {
      request match {
        case GenerateContent(contents, _, _) =>
          val response = GenerateContentResponse(
            candidates = List(
              Candidate(
                content = ResponseContent(
                  parts = List(ResponsePart.Text(s"Streaming response for: ${contents.head}")),
                  role = Some("model")
                ),
                finishReason = FinishReason.STOP,
                safetyRatings = List.empty,
                citationMetadata = None
              )
            ),
            promptFeedback = None
          )
          ZIO.succeed(ZStream.succeed(response.asInstanceOf[Res]))

        case _ =>
          ZIO.succeed(ZStream.fail(GeminiError.InvalidRequest("Unsupported request type")))
      }
    }
  }

  // Test config
  given testConfig: GeminiConfig = GeminiConfig("test-api-key")

  // Test environment
  val testEnv = ZLayer.succeed(new TestHttpClient())

  override def spec = suite("GeminiServiceLive")(
    test("generateContent should return successful response") {
      for {
        service <- ZIO.service[GeminiService[Task]]
        result <- service.generateContent(List(Content.Text("Test input")))
      } yield assertTrue(
        result.isRight &&
        result.toOption.get.candidates.nonEmpty &&
        result.toOption.get.candidates.head.content.parts.head == ResponsePart.Text("Response for: Content.Text(Test input)")
      )
    },

    test("generateContent should use default generation config when none provided") {
      for {
        service <- ZIO.service[GeminiService[Task]]
        result <- service.generateContent(List(Content.Text("Test input")))
      } yield assertTrue(result.isRight)
    },

    test("generateContentStream should return successful stream") {
      for {
        service <- ZIO.service[GeminiService[Task]]
        stream <- service.generateContentStream(List(Content.Text("Test input")))
        result <- stream.runHead
      } yield assertTrue(
        result.isDefined &&
        result.get.candidates.nonEmpty &&
        result.get.candidates.head.content.parts.head == ResponsePart.Text("Streaming response for: Content.Text(Test input)")
      )
    },

    test("countTokens should return token count") {
      for {
        service <- ZIO.service[GeminiService[Task]]
        result <- service.countTokens(List(Content.Text("Test input")))
      } yield assertTrue(
        result.isRight &&
        result.toOption.get == 42
      )
    },

    suite("ZLayer tests")(
      test("layer should create service with provided client") {
        val client = new TestHttpClient()
        val layer = GeminiServiceLive.layer(client)

        for {
          service <- ZIO.service[GeminiService[Task]].provideLayer(layer)
        } yield assertTrue(service.isInstanceOf[GeminiServiceLive])
      },

      test("live should create service from environment") {
        val env = ZLayer.succeed(new TestHttpClient())
        val layer = GeminiServiceLive.live

        for {
          service <- ZIO.service[GeminiService[Task]].provideLayer(layer ++ env)
        } yield assertTrue(service.isInstanceOf[GeminiServiceLive])
      }
    )
  ).provide(
    testEnv >>> GeminiServiceLive.live
  )
} 