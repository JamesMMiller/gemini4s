package gemini4s

import zio._
import zio.stream.ZStream

import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError
import gemini4s.model.GeminiRequest._
import gemini4s.model.GeminiResponse._

/**
 * Core service interface for interacting with the Gemini API.
 * Provides high-level operations for content generation and token counting.
 *
 * This service follows the tagless final pattern, allowing different effect type implementations.
 * The default implementation uses ZIO's Task, but other effect types can be used.
 *
 * Example usage:
 * {{{
 * val service = GeminiService.live // ZLayer-based initialization
 * val config = GeminiConfig("your-api-key")
 * 
 * // Generate content
 * val result = service.generateContent(
 *   contents = List(GeminiService.text("What is the capital of France?")),
 *   safetySettings = None,
 *   generationConfig = None
 * )
 * 
 * // Stream content
 * val stream = service.generateContentStream(
 *   contents = List(GeminiService.text("Tell me a long story")),
 *   safetySettings = None,
 *   generationConfig = None
 * )
 * }}}
 *
 * @tparam F The effect type (e.g., Task for ZIO implementation)
 */
trait GeminiService[F[_]] {
  /**
   * Generates content using the Gemini API.
   *
   * This method sends a single request and receives a complete response.
   * For streaming responses, use [[generateContentStream]].
   *
   * @param contents The input prompts and their contents
   * @param safetySettings Optional safety settings to control content filtering
   * @param generationConfig Optional configuration for content generation
   * @param config The API configuration with credentials
   * @return Either a [[GeminiError]] or a [[GenerateContentResponse]]
   */
  def generateContent(
    contents: List[Content],
    safetySettings: Option[List[SafetySetting]] = None,
    generationConfig: Option[GenerationConfig] = None
  )(using config: GeminiConfig): F[Either[GeminiError, GenerateContentResponse]]

  /**
   * Generates content using the Gemini API with streaming response.
   *
   * This method is useful for generating longer content or when you want
   * to process the response as it arrives. The stream can be interrupted
   * at any time to stop generation.
   *
   * @param contents The input prompts and their contents
   * @param safetySettings Optional safety settings to control content filtering
   * @param generationConfig Optional configuration for content generation
   * @param config The API configuration with credentials
   * @return A stream of [[GenerateContentResponse]] chunks with [[GeminiError]] in error channel
   */
  def generateContentStream(
    contents: List[Content],
    safetySettings: Option[List[SafetySetting]] = None,
    generationConfig: Option[GenerationConfig] = None
  )(using config: GeminiConfig): F[ZStream[Any, GeminiError, GenerateContentResponse]]

  /**
   * Counts tokens in the provided content.
   *
   * This is useful for estimating costs and ensuring content fits within model limits.
   * The Gemini API has a maximum token limit of [[GeminiService.MaxTokensPerRequest]].
   *
   * @param contents The content to analyze
   * @param config The API configuration with credentials
   * @return The number of tokens in the content
   */
  def countTokens(
    contents: List[Content]
  )(using config: GeminiConfig): F[Either[GeminiError, Int]]
}

object GeminiService {
  /** Default model identifier for Gemini Pro */
  val DefaultModel = "gemini-pro"

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
   * This is a convenience method for creating content with a single text part.
   * For more complex content (e.g., with multiple parts or roles), create Content directly.
   *
   * @param text The input text
   * @return A Content instance with the text wrapped in a Part
   */
  def text(text: String): Content = Content(parts = List(Part(text = text)))

  /**
   * API endpoint paths for different operations.
   */
  object Endpoints {
    def generateContent(model: String = DefaultModel): String = s"models/$model:generateContent"
    def generateContentStream(model: String = DefaultModel): String = s"models/$model:streamGenerateContent"
    def countTokens(model: String = DefaultModel): String = s"models/$model:countTokens"
  }
} 