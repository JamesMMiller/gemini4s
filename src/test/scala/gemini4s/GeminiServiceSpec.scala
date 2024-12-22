package gemini4s

import zio._
import zio.json._
import zio.stream.ZStream
import zio.test._
import zio.test.Assertion._

import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError
import gemini4s.model.GeminiRequest.{Content, GenerateContent, GenerationConfig, SafetySetting}
import gemini4s.model.GeminiResponse
import gemini4s.model.GeminiResponse.{Candidate, GenerateContentResponse, PromptFeedback, ResponseContent, ResponsePart}

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
              parts = List(ResponsePart.Text("Test response")),
              role = Some("model")
            ),
            finishReason = GeminiResponse.FinishReason.STOP,
            safetyRatings = List.empty,
            citationMetadata = None
          )
        ),
        promptFeedback = None
      )
      ZIO.succeed(Right(response))
    }

    override def generateContentStream(
      contents: List[Content],
      safetySettings: Option[List[SafetySetting]],
      generationConfig: Option[GenerationConfig]
    )(using config: GeminiConfig): Task[ZStream[Any, GeminiError, GenerateContentResponse]] = {
      val response = GenerateContentResponse(
        candidates = List(
          Candidate(
            content = ResponseContent(
              parts = List(ResponsePart.Text("Streaming test response")),
              role = Some("model")
            ),
            finishReason = GeminiResponse.FinishReason.STOP,
            safetyRatings = List.empty,
            citationMetadata = None
          )
        ),
        promptFeedback = None
      )
      ZIO.succeed(ZStream.succeed(response))
    }

    override def countTokens(
      contents: List[Content]
    )(using config: GeminiConfig): Task[Either[GeminiError, Int]] = {
      // Simulate token count based on content length
      val tokenCount = contents.map {
        case Content.Text(text) => text.split(" ").length
      }.sum
      ZIO.succeed(Right(tokenCount))
    }
  }

  // Test config
  given testConfig: GeminiConfig = GeminiConfig("test-api-key")

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
    },

    // Service implementation tests
    suite("TestGeminiService")(
      test("generateContent should return successful response") {
        val service = new TestGeminiService()
        val content = List(Content.Text("Test input"))

        for {
          result <- service.generateContent(content)
        } yield assertTrue(
          result.isRight &&
          result.toOption.get.candidates.nonEmpty &&
          result.toOption.get.candidates.head.content.parts.head == ResponsePart.Text("Test response")
        )
      },

      test("generateContentStream should return successful stream") {
        val service = new TestGeminiService()
        val content = List(Content.Text("Test input"))

        for {
          stream <- service.generateContentStream(content)
          result <- stream.runHead
        } yield assertTrue(
          result.isDefined &&
          result.get.candidates.nonEmpty &&
          result.get.candidates.head.content.parts.head == ResponsePart.Text("Streaming test response")
        )
      },

      test("countTokens should return token count") {
        val service = new TestGeminiService()
        val content = List(Content.Text("This is a test sentence"))

        for {
          result <- service.countTokens(content)
        } yield assertTrue(
          result.isRight &&
          result.toOption.get == 5 // "This is a test sentence" has 5 words/tokens
        )
      }
    )
  )
} 