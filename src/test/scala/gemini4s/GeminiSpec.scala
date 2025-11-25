package gemini4s

import cats.effect.IO
import fs2.Stream
import munit.CatsEffectSuite

import gemini4s.error.GeminiError
import gemini4s.model.domain._
import gemini4s.model.request._
import gemini4s.model.response._

class GeminiSpec extends CatsEffectSuite {

  // Test implementation of Gemini
  class TestGemini extends Gemini[IO] {

    override def generateContent(
        request: GenerateContentRequest
    ): IO[Either[GeminiError, GenerateContentResponse]] = {
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
        request: GenerateContentRequest
    ): Stream[IO, GenerateContentResponse] = {
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
        request: CountTokensRequest
    ): IO[Either[GeminiError, Int]] =
      // Simulate token counting with a fixed value
      IO.pure(Right(42))

    override def embedContent(
        request: EmbedContentRequest
    ): IO[Either[GeminiError, ContentEmbedding]] = IO.pure(Right(ContentEmbedding(values = List(0.1f, 0.2f))))

    override def batchEmbedContents(
        request: BatchEmbedContentsRequest
    ): IO[Either[GeminiError, List[ContentEmbedding]]] =
      IO.pure(Right(List(ContentEmbedding(values = List(0.1f, 0.2f)))))

    override def createCachedContent(
        request: CreateCachedContentRequest
    ): IO[Either[GeminiError, CachedContent]] = IO.pure(
      Right(
        CachedContent(
          name = "cachedContents/123",
          model = request.model.getOrElse("default"),
          createTime = "now",
          updateTime = "now",
          expireTime = "later"
        )
      )
    )

  }

  test("DefaultModel should be models/gemini-2.5-flash") {
    assertEquals(GeminiConstants.DefaultModel.value, "models/gemini-2.5-flash")
  }

  test("MaxTokensPerRequest should be 30720") {
    assertEquals(GeminiConstants.MaxTokensPerRequest, 30720)
  }

  test("DefaultGenerationConfig should have correct values") {
    val config = GeminiConstants.DefaultGenerationConfig
    assertEquals(config.temperature, Some(GeminiConstants.DefaultTemperature))
    assertEquals(config.topK, Some(GeminiConstants.DefaultTopK))
    assertEquals(config.topP, Some(GeminiConstants.DefaultTopP))
    assertEquals(config.maxOutputTokens, Some(GeminiConstants.MaxTokensPerRequest))
  }

  test("text helper should create Content.Text correctly") {
    val content = Gemini.text("Hello")
    assertEquals(content.parts.head, ContentPart("Hello"))
  }

  test("Endpoints should generate correct paths") {
    val model = ModelName.Gemini25Flash
    assertEquals(GeminiConstants.Endpoints.generateContent(model), "models/gemini-2.5-flash:generateContent")
    assertEquals(
      GeminiConstants.Endpoints.generateContentStream(model),
      "models/gemini-2.5-flash:streamGenerateContent"
    )
    assertEquals(GeminiConstants.Endpoints.countTokens(model), "models/gemini-2.5-flash:countTokens")
    assertEquals(GeminiConstants.Endpoints.embedContent(model), "models/gemini-2.5-flash:embedContent")
    assertEquals(GeminiConstants.Endpoints.batchEmbedContents(model), "models/gemini-2.5-flash:batchEmbedContents")
    assertEquals(GeminiConstants.Endpoints.createCachedContent, "cachedContents")
  }

  test("Endpoints should handle custom model names") {
    val customModel = ModelName.unsafe("models/custom-model")
    assertEquals(GeminiConstants.Endpoints.generateContent(customModel), "models/custom-model:generateContent")
    assertEquals(
      GeminiConstants.Endpoints.generateContentStream(customModel),
      s"${customModel.value}:streamGenerateContent"
    )
    assertEquals(GeminiConstants.Endpoints.countTokens(customModel), s"${customModel.value}:countTokens")
    assertEquals(GeminiConstants.Endpoints.embedContent(customModel), s"${customModel.value}:embedContent")
    assertEquals(
      GeminiConstants.Endpoints.batchEmbedContents(customModel),
      s"${customModel.value}:batchEmbedContents"
    )
  }
}
