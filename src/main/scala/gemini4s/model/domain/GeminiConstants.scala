package gemini4s.model.domain

object GeminiConstants {

  /** Default model identifier for Gemini 2.5 Flash */
  val DefaultModel      = "gemini-2.5-flash"
  val Gemini25Flash     = "gemini-2.5-flash"
  val Gemini25Pro       = "gemini-2.5-pro"
  val Gemini25FlashLite = "gemini-2.5-flash-lite"
  val Gemini3Pro        = "gemini-3-pro-preview"
  val Imagen4           = "imagen-4.0-generate-001"
  val EmbeddingText004  = "text-embedding-004"

  /** Maximum tokens per request (30,720 for Gemini Pro) */
  val MaxTokensPerRequest = 30720

  /** Default temperature for content generation (0.9) */
  val DefaultTemperature = 0.9f

  /** Default top-k value for content generation (10) */
  val DefaultTopK = 10

  /** Default top-p value for content generation (0.9) */
  val DefaultTopP = 0.9f

  /** Default generation configuration with recommended settings */
  val DefaultGenerationConfig = GenerationConfig(
    temperature = Some(DefaultTemperature),
    topK = Some(DefaultTopK),
    topP = Some(DefaultTopP),
    maxOutputTokens = Some(MaxTokensPerRequest)
  )

  /**
   * API endpoint paths for different operations.
   */
  object Endpoints {
    def generateContent(model: String = DefaultModel): String       = s"models/$model:generateContent"
    def generateContentStream(model: String = DefaultModel): String = s"models/$model:streamGenerateContent"
    def countTokens(model: String = DefaultModel): String           = s"models/$model:countTokens"
    def embedContent(model: String = DefaultModel): String          = s"models/$model:embedContent"
    def batchEmbedContents(model: String = DefaultModel): String    = s"models/$model:batchEmbedContents"
    def createCachedContent: String                                 = "cachedContents"
  }

}
