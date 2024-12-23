package gemini4s

import zio._
import zio.json._
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test._

import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError
import gemini4s.model.GeminiRequest._
import gemini4s.model.GeminiResponse
import gemini4s.model.GeminiResponse._

object GeminiServiceSpec extends ZIOSpecDefault {
  // Test implementation of GeminiService
  class TestGeminiService extends GeminiService[Task] {
    override def generateContent(
      contents: List[Content],
      safetySettings: Option[List[SafetySetting]],
      generationConfig: Option[GenerationConfig]
    )(using config: GeminiConfig): Task[Either[GeminiError, GenerateContentResponse]] = {
      // Simulate successful response for test content
      val response = GenerateContentResponse(
        candidates = List(
          Candidate(
            content = ResponseContent(
              parts = List(Part(text = "Test response")),
              role = Some("model")
            ),
            finishReason = Some("STOP"),
            safetyRatings = None
          )
        ),
        usageMetadata = None,
        modelVersion = None
      )
      ZIO.succeed(Right(response))
    }

    override def generateContentStream(
      contents: List[Content],
      safetySettings: Option[List[SafetySetting]],
      generationConfig: Option[GenerationConfig]
    )(using config: GeminiConfig): Task[ZStream[Any, GeminiError, GenerateContentResponse]] = {
      // Simulate successful streaming response
      val responses = List(
        GenerateContentResponse(
          candidates = List(
            Candidate(
              content = ResponseContent(
                parts = List(Part(text = "Streaming test response")),
                role = Some("model")
              ),
              finishReason = Some("STOP"),
              safetyRatings = None
            )
          ),
          usageMetadata = None,
          modelVersion = None
        )
      )
      ZIO.succeed(ZStream.fromIterable(responses))
    }

    override def countTokens(
      contents: List[Content]
    )(using config: GeminiConfig): Task[Either[GeminiError, Int]] = {
      // Simulate token counting with a fixed value
      ZIO.succeed(Right(42))
    }
  }

  def spec = suite("GeminiService")(
    // Companion object tests
    test("DefaultModel should be gemini-pro") {
      assertTrue(GeminiService.DefaultModel == "gemini-pro")
    },
    test("MaxTokensPerRequest should be 30720") {
      assertTrue(GeminiService.MaxTokensPerRequest == 30720)
    },
    test("DefaultGenerationConfig should have correct values") {
      assertTrue(
        GeminiService.DefaultGenerationConfig.temperature.contains(GeminiService.DefaultTemperature) &&
        GeminiService.DefaultGenerationConfig.topK.contains(GeminiService.DefaultTopK) &&
        GeminiService.DefaultGenerationConfig.topP.contains(GeminiService.DefaultTopP) &&
        GeminiService.DefaultGenerationConfig.maxOutputTokens.contains(GeminiService.MaxTokensPerRequest)
      )
    },
    test("text helper should create Content.Text correctly") {
      val content = "test content"
      assertTrue(GeminiService.text(content) == Content.Text(content))
    },
    test("Endpoints should generate correct paths") {
      assertTrue(
        GeminiService.Endpoints.generateContent() == "models/gemini-pro:generateContent" &&
        GeminiService.Endpoints.generateContentStream() == "models/gemini-pro:streamGenerateContent" &&
        GeminiService.Endpoints.countTokens() == "models/gemini-pro:countTokens"
      )
    },
    test("Endpoints should handle custom model names") {
      val customModel = "custom-model"
      assertTrue(
        GeminiService.Endpoints.generateContent(customModel) == s"models/$customModel:generateContent" &&
        GeminiService.Endpoints.generateContentStream(customModel) == s"models/$customModel:streamGenerateContent" &&
        GeminiService.Endpoints.countTokens(customModel) == s"models/$customModel:countTokens"
      )
    }
  )
} 