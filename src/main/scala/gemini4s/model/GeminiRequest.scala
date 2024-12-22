package gemini4s.model

import zio.json._

import GeminiCodecs.given

/**
 * Base trait for all Gemini API requests.
 * Follows the API schema defined in the Gemini API documentation.
 */
sealed trait GeminiRequest {
  def toJson: String = this match {
    case req: GeminiRequest.GenerateContent => req.toJsonPretty
    case req: GeminiRequest.CountTokensRequest => req.toJsonPretty
  }
}

object GeminiRequest {
  /**
   * Request for text generation using the Gemini API.
   *
   * @param contents The list of content parts to generate from
   * @param safetySettings Optional safety settings for content filtering
   * @param generationConfig Optional configuration for text generation
   */
  final case class GenerateContent(
    contents: List[Content],
    safetySettings: Option[List[SafetySetting]] = None,
    generationConfig: Option[GenerationConfig] = None
  ) extends GeminiRequest

  /**
   * Request to count tokens for given content.
   *
   * @param contents The list of content parts to count tokens for
   */
  final case class CountTokensRequest(
    contents: List[Content]
  ) extends GeminiRequest

  /**
   * Content part that can be used in requests.
   */
  sealed trait Content
  object Content {
    final case class Text(text: String) extends Content
  }

  /**
   * Safety setting for content filtering.
   *
   * @param category The harm category to configure
   * @param threshold The safety threshold to apply
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
   *
   * @param temperature Controls randomness in generation
   * @param topK Top-k tokens to consider
   * @param topP Nucleus sampling threshold
   * @param candidateCount Number of candidates to generate
   * @param maxOutputTokens Maximum tokens to generate
   * @param stopSequences Sequences that stop generation
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