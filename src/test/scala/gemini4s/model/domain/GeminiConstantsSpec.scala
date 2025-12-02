package gemini4s.model.domain

import cats.effect.IO
import fs2.Stream
import munit.CatsEffectSuite

import gemini4s.GeminiService
import gemini4s.error.GeminiError
import gemini4s.model.domain._
import gemini4s.model.request._
import gemini4s.model.response._

class GeminiConstantsSpec extends CatsEffectSuite {

  test("DefaultModel should be gemini-flash-latest") {
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

  test("image helper should create Content.InlineData correctly") {
    val base64   = "base64data"
    val mimeType = "image/jpeg"
    val content  = GeminiService.image(base64, mimeType)
    val expected = ContentPart.InlineData(MimeType.unsafe(mimeType), ContentPart.Base64Data(base64))
    assertEquals(content.parts.head, expected)
  }

  test("file helper should create Content.FileData correctly") {
    val uri      = "gs://bucket/file"
    val mimeType = "application/pdf"
    val content  = GeminiService.file(uri, mimeType)
    val expected = ContentPart.FileData(MimeType.unsafe(mimeType), FileUri(uri))
    assertEquals(content.parts.head, expected)
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
    val customModel = ModelName.unsafe("custom-model")
    assertEquals(GeminiConstants.Endpoints.generateContent(customModel), "models/custom-model:generateContent")
    assertEquals(
      GeminiConstants.Endpoints.generateContentStream(customModel),
      "models/custom-model:streamGenerateContent"
    )
    assertEquals(GeminiConstants.Endpoints.countTokens(customModel), "models/custom-model:countTokens")
    assertEquals(GeminiConstants.Endpoints.embedContent(customModel), "models/custom-model:embedContent")
    assertEquals(
      GeminiConstants.Endpoints.batchEmbedContents(customModel),
      "models/custom-model:batchEmbedContents"
    )
  }

  test("Endpoints should handle tuned model names") {
    val tunedModel = ModelName.Tuned("my-tuned-model")
    assertEquals(GeminiConstants.Endpoints.generateContent(tunedModel), "tunedModels/my-tuned-model:generateContent")
  }
}
