package gemini4s.model.domain

object GeminiConstants {

  /** Default model identifier for Gemini 2.5 Flash */
  val DefaultModel: ModelName       = ModelName.Gemini25Flash
  val Gemini25Flash: ModelName      = ModelName.Gemini25Flash
  val Gemini25Pro: ModelName        = ModelName.Gemini25Pro
  val Gemini25FlashLite: ModelName  = ModelName.Gemini25FlashLite
  val Gemini3Pro: ModelName         = ModelName.Gemini3Pro
  val Imagen4: ModelName            = ModelName.Imagen4
  val EmbeddingGemini001: ModelName = ModelName.EmbeddingGemini001

  /** Maximum tokens per request (30,720 for Gemini Pro) */
  val MaxTokensPerRequest = 30720

  /** Default temperature for content generation (0.9) */
  val DefaultTemperature: Temperature = Temperature.unsafe(0.9f)

  /** Default top-k value for content generation (10) */
  val DefaultTopK: TopK = TopK.unsafe(10)

  /** Default top-p value for content generation (0.9) */
  val DefaultTopP: TopP = TopP.unsafe(0.9f)

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
    def generateContent(model: ModelName = DefaultModel): String       = s"${model.value}:generateContent"
    def generateContentStream(model: ModelName = DefaultModel): String = s"${model.value}:streamGenerateContent"
    def countTokens(model: ModelName = DefaultModel): String           = s"${model.value}:countTokens"
    def embedContent(model: ModelName = DefaultModel): String          = s"${model.value}:embedContent"
    def batchEmbedContents(model: ModelName = DefaultModel): String    = s"${model.value}:batchEmbedContents"
    def createCachedContent: String                                    = "cachedContents"
  }

}
