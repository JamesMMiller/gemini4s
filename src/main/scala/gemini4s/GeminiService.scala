package gemini4s

import fs2.Stream

import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError
import gemini4s.model.GeminiRequest._
import gemini4s.model.GeminiResponse._

  /**
   * Core service algebra for interacting with the Google Gemini API.
   *
   * This service follows the Tagless Final pattern, allowing for different effect type implementations.
   * It provides high-level operations for content generation, streaming, and token counting.
   *
   * @tparam F The effect type (e.g., IO for Cats Effect implementation)
   */
  trait GeminiService[F[_]] {

    /**
     * Generates content using the Gemini API.
     *
     * @param contents The input prompts and their contents
     * @param safetySettings Optional safety settings to control content filtering
     * @param generationConfig Optional configuration for content generation
     * @param systemInstruction Optional system instructions
     * @param tools Optional tools available for the model
     * @param toolConfig Optional configuration for tool use
     * @param config The API configuration (implicit)
     * @return Either a [[GeminiError]] or a [[GenerateContentResponse]]
     */
    def generateContent(
        contents: List[Content],
        safetySettings: Option[List[SafetySetting]] = None,
        generationConfig: Option[GenerationConfig] = None,
        systemInstruction: Option[Content] = None,
        tools: Option[List[Tool]] = None,
        toolConfig: Option[ToolConfig] = None
    )(using config: GeminiConfig): F[Either[GeminiError, GenerateContentResponse]]

    /**
     * Generates content using the Gemini API with streaming response.
     *
     * @param contents The input prompts and their contents
     * @param safetySettings Optional safety settings to control content filtering
     * @param generationConfig Optional configuration for content generation
     * @param systemInstruction Optional system instructions
     * @param tools Optional tools available for the model
     * @param toolConfig Optional configuration for tool use
     * @param config The API configuration (implicit)
     * @return A stream of [[GenerateContentResponse]] chunks
     */
    def generateContentStream(
        contents: List[Content],
        safetySettings: Option[List[SafetySetting]] = None,
        generationConfig: Option[GenerationConfig] = None,
        systemInstruction: Option[Content] = None,
        tools: Option[List[Tool]] = None,
        toolConfig: Option[ToolConfig] = None
    )(using config: GeminiConfig): Stream[F, GenerateContentResponse]

    /**
     * Counts tokens in the provided content.
     *
     * @param contents The content to analyze
     * @param config The API configuration (implicit)
     * @return The number of tokens
     */
    def countTokens(
        contents: List[Content]
    )(using config: GeminiConfig): F[Either[GeminiError, Int]]

  }

  object GeminiService {

    /** Default model identifier for Gemini 2.5 Flash */
    val DefaultModel = "gemini-2.5-flash"
    val Gemini25Flash = "gemini-2.5-flash"
    val Gemini25Pro = "gemini-2.5-pro"
    val Gemini25FlashLite = "gemini-2.5-flash-lite"
    val Gemini3Pro = "gemini-3-pro-preview"
    val Imagen4 = "imagen-4.0-generate-001"
    val EmbeddingText004 = "text-embedding-004"

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
     * Creates a Content instance from text input.
     *
     * @param text The input text
     * @return A Content instance with the text wrapped in a Part
     */
    def text(text: String): Content = Content(parts = List(Part(text = text)))

    /**
     * API endpoint paths for different operations.
     */
    object Endpoints {
      def generateContent(model: String = DefaultModel): String       = s"models/$model:generateContent"
      def generateContentStream(model: String = DefaultModel): String = s"models/$model:streamGenerateContent"
      def countTokens(model: String = DefaultModel): String           = s"models/$model:countTokens"
      def embedContent(model: String = DefaultModel): String          = s"models/$model:embedContent"
      def batchEmbedContents(model: String = DefaultModel): String    = s"models/$model:batchEmbedContents"
    }

  }
