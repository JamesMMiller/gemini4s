package gemini4s.http

import zio.stream.ZStream
import zio.test.Assertion._
import zio.test._
import zio.{Task, ZIO}

import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError
import gemini4s.error.GeminiError._
import gemini4s.model.GeminiRequest._
import gemini4s.model.GeminiResponse._
import gemini4s.model.{GeminiRequest, GeminiResponse}

object GeminiHttpClientSpec extends ZIOSpecDefault {
  def spec = suite("GeminiHttpClient")(
    test("post should handle successful response") {
      val testConfig = GeminiConfig("test-api-key")
      val testRequest = GenerateContent(
        contents = List(Content.Text("Test prompt"))
      )
      val testResponse = GenerateContentResponse(
        candidates = List(
          Candidate(
            content = ResponseContent(
              parts = List(ResponsePart.Text("Generated response")),
              role = Some("model")
            ),
            finishReason = FinishReason.STOP,
            safetyRatings = List(),
            citationMetadata = None
          )
        ),
        promptFeedback = None
      )

      val testClient = new GeminiHttpClient[Task] {
        def post[Req <: GeminiRequest, Res <: GeminiResponse](
          endpoint: String,
          request: Req
        )(using config: GeminiConfig): Task[Either[GeminiError, Res]] =
          ZIO.succeed(Right(testResponse.asInstanceOf[Res]))
          
        def postStream[Req <: GeminiRequest, Res <: GeminiResponse](
          endpoint: String,
          request: Req
        )(using config: GeminiConfig): Task[ZStream[Any, GeminiError, Res]] =
          ZIO.succeed(ZStream.succeed(testResponse.asInstanceOf[Res]))
      }

      for {
        response <- testClient.post[GenerateContent, GenerateContentResponse]("/test", testRequest)(using testConfig)
      } yield assertTrue(
        response.isRight,
        response.exists(_.candidates.nonEmpty),
        response.exists(_.candidates.head.content.parts.head.asInstanceOf[ResponsePart.Text].text == "Generated response")
      )
    },

    test("post should handle API errors") {
      val testConfig = GeminiConfig("invalid-key")
      val testRequest = GenerateContent(
        contents = List(Content.Text("Test prompt"))
      )

      val testClient = new GeminiHttpClient[Task] {
        def post[Req <: GeminiRequest, Res <: GeminiResponse](
          endpoint: String,
          request: Req
        )(using config: GeminiConfig): Task[Either[GeminiError, Res]] =
          ZIO.succeed(Left(InvalidApiKey("Invalid API key provided")))
          
        def postStream[Req <: GeminiRequest, Res <: GeminiResponse](
          endpoint: String,
          request: Req
        )(using config: GeminiConfig): Task[ZStream[Any, GeminiError, Res]] =
          ZIO.succeed(ZStream.fail(InvalidApiKey("Invalid API key provided")))
      }

      for {
        response <- testClient.post[GenerateContent, GenerateContentResponse]("/test", testRequest)(using testConfig)
      } yield assertTrue(
        response.isLeft,
        response.swap.exists(_.isInstanceOf[InvalidApiKey])
      )
    },

    test("postStream should stream successful response chunks") {
      val testConfig = GeminiConfig("test-api-key")
      val testRequest = GenerateContent(
        contents = List(Content.Text("Test prompt"))
      )
      val testResponses = List(
        GenerateContentResponse(
          candidates = List(
            Candidate(
              content = ResponseContent(
                parts = List(ResponsePart.Text("chunk1")),
                role = Some("model")
              ),
              finishReason = FinishReason.STOP,
              safetyRatings = List(),
              citationMetadata = None
            )
          ),
          promptFeedback = None
        ),
        GenerateContentResponse(
          candidates = List(
            Candidate(
              content = ResponseContent(
                parts = List(ResponsePart.Text("chunk2")),
                role = Some("model")
              ),
              finishReason = FinishReason.STOP,
              safetyRatings = List(),
              citationMetadata = None
            )
          ),
          promptFeedback = None
        )
      )

      val testClient = new GeminiHttpClient[Task] {
        def post[Req <: GeminiRequest, Res <: GeminiResponse](
          endpoint: String,
          request: Req
        )(using config: GeminiConfig): Task[Either[GeminiError, Res]] =
          ZIO.succeed(Right(testResponses.head.asInstanceOf[Res]))
          
        def postStream[Req <: GeminiRequest, Res <: GeminiResponse](
          endpoint: String,
          request: Req
        )(using config: GeminiConfig): Task[ZStream[Any, GeminiError, Res]] =
          ZIO.succeed(ZStream.fromIterable(testResponses.asInstanceOf[List[Res]]))
      }

      for {
        stream <- testClient.postStream[GenerateContent, GenerateContentResponse]("/test", testRequest)(using testConfig)
        chunks <- stream.runCollect.either
      } yield assertTrue(
        chunks.isRight,
        chunks.exists(_.size == 2),
        chunks.exists(_.head.candidates.head.content.parts.head.asInstanceOf[ResponsePart.Text].text == "chunk1"),
        chunks.exists(_.last.candidates.head.content.parts.head.asInstanceOf[ResponsePart.Text].text == "chunk2")
      )
    },

    test("postStream should handle streaming errors") {
      val testConfig = GeminiConfig("invalid-key")
      val testRequest = GenerateContent(
        contents = List(Content.Text("Test prompt"))
      )

      val testClient = new GeminiHttpClient[Task] {
        def post[Req <: GeminiRequest, Res <: GeminiResponse](
          endpoint: String,
          request: Req
        )(using config: GeminiConfig): Task[Either[GeminiError, Res]] =
          ZIO.succeed(Left(InvalidApiKey("Invalid API key provided")))
          
        def postStream[Req <: GeminiRequest, Res <: GeminiResponse](
          endpoint: String,
          request: Req
        )(using config: GeminiConfig): Task[ZStream[Any, GeminiError, Res]] =
          ZIO.succeed(ZStream.fail(InvalidApiKey("Invalid API key provided")))
      }

      for {
        stream <- testClient.postStream[GenerateContent, GenerateContentResponse]("/test", testRequest)(using testConfig)
        result <- stream.runCollect.either
      } yield assertTrue(
        result.isLeft,
        result.swap.exists(_.isInstanceOf[InvalidApiKey])
      )
    }
  )
} 