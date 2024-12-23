package gemini4s.model

import zio.json._

/**
 * Base trait for all Gemini API requests.
 */
trait GeminiRequest

object GeminiRequest {
  /**
   * Request for text generation using the Gemini API.
   */
  final case class GenerateContent(
    contents: List[Content],
    safetySettings: Option[List[SafetySetting]] = None,
    generationConfig: Option[GenerationConfig] = None
  ) extends GeminiRequest

  /**
   * Request to count tokens for given content.
   */
  final case class CountTokensRequest(
    contents: List[Content]
  ) extends GeminiRequest

  /**
   * Content part that can be used in requests.
   */
  final case class Content(
    parts: List[Part],
    role: Option[String] = None
  )

  /**
   * A part of the content.
   */
  final case class Part(text: String)

  /**
   * Safety setting for content filtering.
   */
  final case class SafetySetting(
    category: HarmCategory,
    threshold: HarmBlockThreshold
  )

  /**
   * Categories of potential harm in generated content.
   */
  enum HarmCategory {
    case HARM_CATEGORY_UNSPECIFIED
    case HARASSMENT
    case HATE_SPEECH
    case SEXUALLY_EXPLICIT
    case DANGEROUS_CONTENT
  }

  /**
   * Thresholds for blocking harmful content.
   */
  enum HarmBlockThreshold {
    case HARM_BLOCK_THRESHOLD_UNSPECIFIED
    case BLOCK_LOW_AND_ABOVE
    case BLOCK_MEDIUM_AND_ABOVE
    case BLOCK_ONLY_HIGH
    case BLOCK_NONE
  }

  /**
   * Configuration for text generation.
   */
  final case class GenerationConfig(
    temperature: Option[Float] = None,
    topK: Option[Int] = None,
    topP: Option[Float] = None,
    candidateCount: Option[Int] = None,
    maxOutputTokens: Option[Int] = None,
    stopSequences: Option[List[String]] = None
  )
} 