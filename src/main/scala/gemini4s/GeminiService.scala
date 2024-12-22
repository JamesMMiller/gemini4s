package gemini4s

import zio.stream.ZStream

import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError
import gemini4s.model.GeminiRequest.{Content, GenerateContent, GenerationConfig, SafetySetting}
import gemini4s.model.GeminiResponse.GenerateContentResponse

/**
 * Core algebra for the Gemini API service.
 * Provides high-level operations for interacting with the Gemini API.
 * Uses tagless final pattern for flexibility in effect type.
 *
 * @tparam F The effect type
 */
trait GeminiService[F[_]] {
  /**
   * Generates content using the Gemini API.
   *
   * @param contents The list of content parts to generate from
   * @param safetySettings Optional safety settings for content filtering
   * @param generationConfig Optional configuration for text generation
   * @return Generated content response wrapped in effect F with GeminiError in error channel
   */
  def generateContent(
    contents: List[Content],
    safetySettings: Option[List[SafetySetting]] = None,
    generationConfig: Option[GenerationConfig] = None
  )(using config: GeminiConfig): F[Either[GeminiError, GenerateContentResponse]]

  /**
   * Generates content using the Gemini API with streaming response.
   *
   * @param contents The list of content parts to generate from
   * @param safetySettings Optional safety settings for content filtering
   * @param generationConfig Optional configuration for text generation
   * @return Stream of generated content responses with GeminiError in error channel
   */
  def generateContentStream(
    contents: List[Content],
    safetySettings: Option[List[SafetySetting]] = None,
    generationConfig: Option[GenerationConfig] = None
  )(using config: GeminiConfig): F[ZStream[Any, GeminiError, GenerateContentResponse]]

  /**
   * Counts tokens for the given content.
   * This is useful for staying within model context limits.
   *
   * @param contents The list of content parts to count tokens for
   * @return Token count wrapped in effect F with GeminiError in error channel
   */
  def countTokens(
    contents: List[Content]
  )(using config: GeminiConfig): F[Either[GeminiError, Int]]
}

object GeminiService {
  /** Default model version for content generation */
  val DefaultModel = "gemini-pro"

  /** Maximum tokens allowed per request */
  val MaxTokensPerRequest = 30720

  /** Default temperature for content generation */
  val DefaultTemperature = 0.9f

  /** Default top-k value for content generation */
  val DefaultTopK = 40

  /** Default top-p value for content generation */
  val DefaultTopP = 0.95f

  /** API endpoints */
  object Endpoints {
    def generateContent(model: String = DefaultModel) = s"models/$model:generateContent"
    def generateContentStream(model: String = DefaultModel) = s"models/$model:streamGenerateContent"
    def countTokens(model: String = DefaultModel) = s"models/$model:countTokens"
  }

  /** Default generation config with recommended settings */
  val DefaultGenerationConfig = GenerationConfig(
    temperature = Some(DefaultTemperature),
    topK = Some(DefaultTopK),
    topP = Some(DefaultTopP),
    maxOutputTokens = Some(MaxTokensPerRequest)
  )

  /** Helper to create a text content part */
  def text(content: String): Content = Content.Text(content)
} 