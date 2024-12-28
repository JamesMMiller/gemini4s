package gemini4s.examples

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream.ZStream

import gemini4s.GeminiService
import gemini4s.config.GeminiConfig
import gemini4s.model.GeminiRequest._
import gemini4s.model.GeminiResponse._
import gemini4s.model.GeminiError

object GeminiExamplesSpec extends ZIOSpecDefault {
  def spec = suite("GeminiExamples")(
    suite("errorHandlingExample")(
      test("should handle rate limit errors with retry") {
        val mockService = new GeminiService[Task] {
          private var attempts = 0
          
          def generateContent(contents: List[Content], safetySettings: Option[List[SafetySetting]] = None, generationConfig: Option[GenerationConfig] = None)(using config: GeminiConfig) = {
            attempts += 1
            if (attempts <= 2) ZIO.fail(GeminiError.RateLimitError("Rate limit exceeded"))
            else ZIO.succeed(Right(successResponse))
          }
          
          def generateContentStream(contents: List[Content], safetySettings: Option[List[SafetySetting]] = None, generationConfig: Option[GenerationConfig] = None)(using config: GeminiConfig) = 
            ZIO.succeed(ZStream.empty)
          
          def countTokens(contents: List[Content])(using config: GeminiConfig) = 
            ZIO.succeed(Right(100))
        }

        val result = GeminiExamples.errorHandlingExample("test-key")
          .provide(
            ZLayer.succeed(mockService)
          )

        result.map(_ => assertCompletes)
      },
      
      test("should handle network errors with fallback") {
        val mockService = new GeminiService[Task] {
          def generateContent(contents: List[Content], safetySettings: Option[List[SafetySetting]] = None, generationConfig: Option[GenerationConfig] = None)(using config: GeminiConfig) = 
            ZIO.fail(GeminiError.NetworkError("Connection failed", None))
          
          def generateContentStream(contents: List[Content], safetySettings: Option[List[SafetySetting]] = None, generationConfig: Option[GenerationConfig] = None)(using config: GeminiConfig) = 
            ZIO.succeed(ZStream.empty)
          
          def countTokens(contents: List[Content])(using config: GeminiConfig) = 
            ZIO.succeed(Right(100))
        }

        val result = GeminiExamples.errorHandlingExample("test-key")
          .provide(
            ZLayer.succeed(mockService)
          )

        result.map(_ => assertCompletes)
      }
    ),
    
    suite("combinedFeaturesExample")(
      test("should combine token counting and streaming with safety checks") {
        val mockService = new GeminiService[Task] {
          def generateContent(contents: List[Content], safetySettings: Option[List[SafetySetting]] = None, generationConfig: Option[GenerationConfig] = None)(using config: GeminiConfig) = 
            ZIO.succeed(Right(successResponse))
          
          def generateContentStream(contents: List[Content], safetySettings: Option[List[SafetySetting]] = None, generationConfig: Option[GenerationConfig] = None)(using config: GeminiConfig) = {
            assert(safetySettings.isDefined, "Safety settings should be provided")
            assert(generationConfig.isDefined, "Generation config should be provided")
            ZIO.succeed(ZStream.fromIterable(List(successResponse)))
          }
          
          def countTokens(contents: List[Content])(using config: GeminiConfig) = 
            ZIO.succeed(Right(150))
        }

        val result = GeminiExamples.combinedFeaturesExample("test-key", "test prompt")
          .provide(
            ZLayer.succeed(mockService)
          )

        result.map(_ => assertCompletes)
      },
      
      test("should handle stream errors gracefully") {
        val mockService = new GeminiService[Task] {
          def generateContent(contents: List[Content], safetySettings: Option[List[SafetySetting]] = None, generationConfig: Option[GenerationConfig] = None)(using config: GeminiConfig) = 
            ZIO.succeed(Right(successResponse))
          
          def generateContentStream(contents: List[Content], safetySettings: Option[List[SafetySetting]] = None, generationConfig: Option[GenerationConfig] = None)(using config: GeminiConfig) = 
            ZIO.succeed(ZStream.fail(GeminiError.StreamError("Stream failed")))
          
          def countTokens(contents: List[Content])(using config: GeminiConfig) = 
            ZIO.succeed(Right(150))
        }

        val result = GeminiExamples.combinedFeaturesExample("test-key", "test prompt")
          .provide(
            ZLayer.succeed(mockService)
          )

        result.map(_ => assertCompletes)
      }
    )
  )

  private val successResponse = GenerateContentResponse(
    candidates = List(
      Candidate(
        content = ResponseContent(
          parts = List(ResponsePart(text = "Test response")),
          role = "model"
        ),
        finishReason = FinishReason.STOP,
        safetyRatings = List(
          SafetyRating(
            category = HarmCategory.HARASSMENT,
            probability = HarmProbability.NEGLIGIBLE
          )
        ),
        citationMetadata = None,
        tokenCount = 100
      )
    ),
    promptFeedback = None
  )
} 