package gemini4s

import cats.effect.IO
import fs2.Stream
import munit.CatsEffectSuite

import gemini4s.error.GeminiError
import gemini4s.model.domain._
import gemini4s.model.request._
import gemini4s.model.response._

class GeminiServiceSpec extends CatsEffectSuite {

  // Test implementation of Gemini
  class TestGemini extends GeminiService[IO] {

    override def generateContent(
        request: GenerateContentRequest
    ): IO[Either[GeminiError, GenerateContentResponse]] = {
      // Simulate successful response for test content
      val response = GenerateContentResponse(
        candidates = List(
          Candidate(
            content = Some(
              ResponseContent(
                parts = List(ResponsePart.Text(text = "Test response")),
                role = Some("model")
              )
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
            content = Some(
              ResponseContent(
                parts = List(ResponsePart.Text(text = "Streaming test response")),
                role = Some("model")
              )
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
    assertEquals(GeminiConstants.DefaultModel.value, "gemini-flash-latest")
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
    val content = GeminiService.text("Hello")
    assertEquals(content.parts.head, ContentPart.Text("Hello"))
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
  test("GeminiService should handle error responses") {
    val service = new TestGemini {
      override def generateContent(request: GenerateContentRequest): IO[Either[GeminiError, GenerateContentResponse]] =
        IO.pure(Left(GeminiError.InvalidRequest("Invalid")))
    }

    service.generateContent(GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("test")))).map {
      case Left(e)  => assertEquals(e.message, "Invalid")
      case Right(_) => fail("Expected error")
    }
  }

  test("GeminiService should support countTokens") {
    val service = new TestGemini
    service.countTokens(CountTokensRequest(ModelName.Gemini25Flash, List(GeminiService.text("test")))).map {
      case Right(count) => assertEquals(count, 42)
      case Left(_)      => fail("Expected success")
    }
  }

  test("GeminiService should support embedContent") {
    val service = new TestGemini
    service.embedContent(EmbedContentRequest(GeminiService.text("test"), ModelName.Gemini25Flash)).map {
      case Right(embedding) => assertEquals(embedding.values, List(0.1f, 0.2f))
      case Left(_)          => fail("Expected success")
    }
  }

  test("GeminiService should support batchEmbedContents") {
    val service = new TestGemini
    service.batchEmbedContents(BatchEmbedContentsRequest(ModelName.Gemini25Flash, List.empty)).map {
      case Right(embeddings) => assertEquals(embeddings.head.values, List(0.1f, 0.2f))
      case Left(_)           => fail("Expected success")
    }
  }

  test("GeminiService should support createCachedContent") {
    val service = new TestGemini
    service.createCachedContent(CreateCachedContentRequest(model = Some("model"))).map {
      case Right(cache) => assertEquals(cache.name, "cachedContents/123")
      case Left(_)      => fail("Expected success")
    }
  }
}
