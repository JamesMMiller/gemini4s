package gemini4s.model.domain

object GeminiConstants {

  /** Default model identifier for Gemini Flash Latest */
  val DefaultModel: ModelName       = ModelName.GeminiFlashLatest
  val Gemini25Flash: ModelName      = ModelName.Gemini25Flash
  val Gemini25Pro: ModelName        = ModelName.Gemini25Pro
  val Gemini25FlashLite: ModelName  = ModelName.Gemini25FlashLite
  val Gemini3Pro: ModelName         = ModelName.Gemini3Pro
  val Imagen4: ModelName            = ModelName.Imagen4
  val EmbeddingGemini001: ModelName = ModelName.EmbeddingGemini001
  val GeminiProLatest: ModelName    = ModelName.GeminiProLatest
  val GeminiFlashLatest: ModelName  = ModelName.GeminiFlashLatest

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

    private def toApiString(model: ModelName): String = model match {
      case ModelName.Standard(v) => s"models/$v"
      case ModelName.Tuned(v)    => s"tunedModels/$v"
    }

    def generateContent(model: ModelName = DefaultModel): String       = s"${toApiString(model)}:generateContent"
    def generateContentStream(model: ModelName = DefaultModel): String = s"${toApiString(model)}:streamGenerateContent"
    def countTokens(model: ModelName = DefaultModel): String           = s"${toApiString(model)}:countTokens"
    def embedContent(model: ModelName = DefaultModel): String          = s"${toApiString(model)}:embedContent"
    def batchEmbedContents(model: ModelName): String                   = s"${toApiString(model)}:batchEmbedContents"
    def batchGenerateContent(model: ModelName): String                 = s"${toApiString(model)}:batchGenerateContent"
    def getBatchJob(name: String): String                              = name
    val listBatchJobs: String                                          = "batches"
    def cancelBatchJob(name: String): String                           = s"$name:cancel"
    def deleteBatchJob(name: String): String                           = name
    val createCachedContent: String                                    = "cachedContents"

    // File API
    val uploadFile: String = "https://generativelanguage.googleapis.com/upload/v1beta/files"
    val files: String      = "files"
  }

}
