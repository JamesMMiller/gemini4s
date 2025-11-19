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
   * @param config The API configuration (implicit)
   * @return Either a [[GeminiError]] or a [[GenerateContentResponse]]
   */
  def generateContent(
      contents: List[Content],
      safetySettings: Option[List[SafetySetting]] = None,
      generationConfig: Option[GenerationConfig] = None,
      systemInstruction: Option[Content] = None
  )(using config: GeminiConfig): F[Either[GeminiError, GenerateContentResponse]]

  /**
   * Generates content using the Gemini API with streaming response.
   *
   * @param contents The input prompts and their contents
   * @param safetySettings Optional safety settings to control content filtering
   * @param generationConfig Optional configuration for content generation
   * @param systemInstruction Optional system instructions
   * @param config The API configuration (implicit)
   * @return A stream of [[GenerateContentResponse]] chunks
   */
  def generateContentStream(
      contents: List[Content],
      safetySettings: Option[List[SafetySetting]] = None,
      generationConfig: Option[GenerationConfig] = None,
      systemInstruction: Option[Content] = None
  )(using config: GeminiConfig): Stream[F, Either[GeminiError, GenerateContentResponse]]

  /**
   * Counts tokens in the provided content.
   *
   * @param contents The content to analyze
   * @param config The API configuration (implicit)
   * @return The number of tokens
   */
  def countTokens(
      contents: List[Content]
  )(using config: GeminiConfig): F[Either[GeminiError, CountTokensResponse]]

}

object GeminiService {

  /** Default model identifier for Gemini 2.5 Flash */
  val DefaultModel = "gemini-2.5-flash"

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
  def text(text: String): Content = Content(parts = List(Part.Text(text)))

  /**
   * Creates a Content instance from image data.
   *
   * @param mimeType The MIME type of the image (e.g., "image/jpeg")
   * @param base64Data The base64 encoded image data
   * @return A Content instance with the image wrapped in a Part
   */
  def image(mimeType: String, base64Data: String): Content =
    Content(parts = List(Part.InlineData(Part.Blob(mimeType, base64Data))))

  /**
   * API endpoint paths for different operations.
   */
  object Endpoints {
    def generateContent(model: String = DefaultModel): String       = s"models/$model:generateContent"
    def generateContentStream(model: String = DefaultModel): String = s"models/$model:streamGenerateContent"
    def countTokens(model: String = DefaultModel): String           = s"models/$model:countTokens"
  }

}
