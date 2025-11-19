package gemini4s

import cats.effect.IO
import fs2.Stream
import munit.CatsEffectSuite

import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError
import gemini4s.model.GeminiRequest._
import gemini4s.model.GeminiResponse
import gemini4s.model.GeminiResponse._

class GeminiServiceSpec extends CatsEffectSuite {

  // Test implementation of GeminiService
  class TestGeminiService extends GeminiService[IO] {

    override def generateContent(
        contents: List[Content],
        safetySettings: Option[List[SafetySetting]],
        generationConfig: Option[GenerationConfig],
        systemInstruction: Option[Content],
        tools: Option[List[Tool]],
        toolConfig: Option[ToolConfig]
    )(using config: GeminiConfig): IO[Either[GeminiError, GenerateContentResponse]] = {
      // Simulate successful response for test content
      val response = GenerateContentResponse(
        candidates = List(
          Candidate(
            content = ResponseContent(
              parts = List(ResponsePart.Text(text = "Test response")),
              role = Some("model")
            ),
            finishReason = Some("STOP"),
            safetyRatings = None
          )
        ),
        usageMetadata = None,
        modelVersion = None
      )
      IO.pure(Right(response))
    }

    override def generateContentStream(
        contents: List[Content],
        safetySettings: Option[List[SafetySetting]],
        generationConfig: Option[GenerationConfig],
        systemInstruction: Option[Content],
        tools: Option[List[Tool]],
        toolConfig: Option[ToolConfig]
    )(using config: GeminiConfig): Stream[IO, GenerateContentResponse] = {
      // Simulate successful streaming response
      val response = GenerateContentResponse(
        candidates = List(
          Candidate(
            content = ResponseContent(
              parts = List(ResponsePart.Text(text = "Streaming test response")),
              role = Some("model")
            ),
            finishReason = Some("STOP"),
            safetyRatings = None
          )
        ),
        usageMetadata = None,
        modelVersion = None
      )
      Stream.emit(response)
    }

    override def countTokens(
        contents: List[Content]
    )(using config: GeminiConfig): IO[Either[GeminiError, Int]] =
      // Simulate token counting with a fixed value
      IO.pure(Right(42))

    override def embedContent(
        content: Content,
        taskType: Option[TaskType],
        title: Option[String],
        outputDimensionality: Option[Int]
    )(using config: GeminiConfig): IO[Either[GeminiError, ContentEmbedding]] =
      IO.pure(Right(ContentEmbedding(values = List(0.1f, 0.2f))))

    override def batchEmbedContents(
        requests: List[EmbedContentRequest]
    )(using config: GeminiConfig): IO[Either[GeminiError, List[ContentEmbedding]]] =
      IO.pure(Right(List(ContentEmbedding(values = List(0.1f, 0.2f)))))

    override def createCachedContent(
        model: String,
        systemInstruction: Option[Content],
        contents: Option[List[Content]],
        tools: Option[List[Tool]],
        toolConfig: Option[ToolConfig],
        ttl: Option[String],
        displayName: Option[String]
    )(using config: GeminiConfig): IO[Either[GeminiError, CachedContent]] =
      IO.pure(Right(CachedContent(
        name = "cachedContents/123",
        model = model,
        createTime = "now",
        updateTime = "now",
        expireTime = "later"
      )))

  }

  test("DefaultModel should be gemini-2.5-flash") {
    assertEquals(GeminiService.DefaultModel, "gemini-2.5-flash")
  }

  test("MaxTokensPerRequest should be 30720") {
    assertEquals(GeminiService.MaxTokensPerRequest, 30720)
  }

  test("DefaultGenerationConfig should have correct values") {
    assert(GeminiService.DefaultGenerationConfig.temperature.contains(GeminiService.DefaultTemperature))
    assert(GeminiService.DefaultGenerationConfig.topK.contains(GeminiService.DefaultTopK))
    assert(GeminiService.DefaultGenerationConfig.topP.contains(GeminiService.DefaultTopP))
    assert(GeminiService.DefaultGenerationConfig.maxOutputTokens.contains(GeminiService.MaxTokensPerRequest))
  }

  test("text helper should create Content.Text correctly") {
    val content = "test content"
    assertEquals(GeminiService.text(content), Content(parts = List(Part(text = content))))
  }

  test("Endpoints should generate correct paths") {
    assertEquals(GeminiService.Endpoints.generateContent(), "models/gemini-2.5-flash:generateContent")
    assertEquals(GeminiService.Endpoints.generateContentStream(), "models/gemini-2.5-flash:streamGenerateContent")
    assertEquals(GeminiService.Endpoints.countTokens(), "models/gemini-2.5-flash:countTokens")
    assertEquals(GeminiService.Endpoints.embedContent(), "models/gemini-2.5-flash:embedContent")
    assertEquals(GeminiService.Endpoints.batchEmbedContents(), "models/gemini-2.5-flash:batchEmbedContents")
    assertEquals(GeminiService.Endpoints.createCachedContent, "cachedContents")
  }

  test("Endpoints should handle custom model names") {
    val customModel = "custom-model"
    assertEquals(GeminiService.Endpoints.generateContent(customModel), s"models/$customModel:generateContent")
    assertEquals(
      GeminiService.Endpoints.generateContentStream(customModel),
      s"models/$customModel:streamGenerateContent"
    )
    assertEquals(GeminiService.Endpoints.countTokens(customModel), s"models/$customModel:countTokens")
    assertEquals(GeminiService.Endpoints.embedContent(customModel), s"models/$customModel:embedContent")
    assertEquals(GeminiService.Endpoints.batchEmbedContents(customModel), s"models/$customModel:batchEmbedContents")
  }
}
