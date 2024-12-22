package gemini4s.http

import zio.Exit.Failure
import zio._
import zio.json._
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test._

import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError._
import gemini4s.error._
import gemini4s.model.GeminiCodecs.given
import gemini4s.model.GeminiRequest._
import gemini4s.model.GeminiResponse._
import gemini4s.model._

object GeminiHttpClientSpec extends ZIOSpecDefault {
  class MockGeminiHttpClient(
    postResponse: Either[GeminiError, GenerateContentResponse],
    streamResponses: Either[GeminiError, List[GenerateContentResponse]]
  ) extends GeminiHttpClient[[A] =>> IO[GeminiError, A]] {

    override def post[Req <: GeminiRequest, Res <: GeminiResponse](endpoint: String, request: Req)(using config: GeminiConfig): IO[GeminiError, Either[GeminiError, Res]] = {
      postResponse match {
        case Right(response) => ZIO.succeed(Right(response.asInstanceOf[Res]))
        case Left(error) => ZIO.succeed(Left(error))
      }
    }

    override def postStream[Req <: GeminiRequest, Res <: GeminiResponse](endpoint: String, request: Req)(using config: GeminiConfig): IO[GeminiError, ZStream[Any, GeminiError, Res]] = {
      streamResponses match {
        case Right(responses) => ZIO.succeed(ZStream.fromIterable(responses.asInstanceOf[List[Res]]))
        case Left(error) => ZIO.succeed(ZStream.fail(error))
      }
    }
  }

  given GeminiConfig = GeminiConfig("test-api-key", "https://test.example.com")

  def spec = suite("GeminiHttpClient")(
    test("post should handle successful response") {
      val request = GenerateContent(contents = List(Content.Text("Test prompt")))
      val response = GenerateContentResponse(
        candidates = List(
          Candidate(
            content = ResponseContent(
              parts = List(ResponsePart.Text("Generated text")),
              role = Some("model")
            ),
            finishReason = FinishReason.STOP,
            safetyRatings = List(SafetyRating(
              category = HarmCategory.HARASSMENT,
              probability = HarmProbability.LOW
            )),
            citationMetadata = None
          )
        ),
        promptFeedback = None
      )

      val mockClient = MockGeminiHttpClient(Right(response), Right(List.empty))

      assertZIO(
        mockClient.post[GenerateContent, GenerateContentResponse]("/test", request).exit
      )(succeeds(isRight(equalTo(response))))
    },

    test("postStream should stream successful response chunks") {
      val request = GenerateContent(contents = List(Content.Text("Test prompt")))
      val responses = List(
        GenerateContentResponse(
          candidates = List(Candidate(
            content = ResponseContent(parts = List(ResponsePart.Text("First")), role = Some("model")),
            finishReason = FinishReason.STOP,
            safetyRatings = List(SafetyRating(category = HarmCategory.HARASSMENT, probability = HarmProbability.LOW)),
            citationMetadata = None
          )),
          promptFeedback = None
        ),
        GenerateContentResponse(
          candidates = List(Candidate(
            content = ResponseContent(parts = List(ResponsePart.Text("Second")), role = Some("model")),
            finishReason = FinishReason.STOP,
            safetyRatings = List.empty,
            citationMetadata = None
          )),
          promptFeedback = None
        )
      )

      val mockClient = MockGeminiHttpClient(Right(responses.head), Right(responses))

      assertZIO(
        mockClient.postStream[GenerateContent, GenerateContentResponse]("/test", request)
          .flatMap(_.runCollect).exit
      )(succeeds(equalTo(Chunk.fromIterable(responses))))
    },

    test("error types should have proper messages and causes") {
      val cause = new RuntimeException("Test error")
      val errors = List(
        StreamInitializationError("Init failed", None) -> "Init failed",
        StreamInterrupted("Interrupted", Some(cause)) -> "Interrupted",
        StreamInitializationError("With cause", Some(cause)) -> "With cause"
      )

      assertTrue(
        errors.forall { case (error, msg) => 
          error.getMessage == msg && 
          error.getCause == error.cause.getOrElse(null)
        }
      )
    }
  )
} 