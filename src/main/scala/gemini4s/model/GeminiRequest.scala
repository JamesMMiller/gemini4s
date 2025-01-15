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
   * 
   * @param temperature Controls the randomness of the output (0.0 to 1.0).
   *                    Values closer to 1.0 produce more varied/creative responses,
   *                    while values closer to 0.0 produce more focused/deterministic responses.
   *                    Default is 0.9.
   * 
   * @param topK Maximum number of tokens to consider when sampling.
   *             The model will only consider the top K most probable tokens.
   *             Works in combination with topP for sampling.
   *             Default is 10.
   * 
   * @param topP Maximum cumulative probability for nucleus sampling (0.0 to 1.0).
   *             The model considers tokens until their cumulative probability reaches this value.
   *             Works with topK for a combined sampling approach.
   *             Default is 0.9.
   * 
   * @param candidateCount Number of alternative responses to generate (1 to 8).
   *                       Higher values give you multiple different completions.
   *                       Default is 1.
   * 
   * @param maxOutputTokens Maximum number of tokens to generate in the response.
   *                        Default is 30,720 for Gemini Pro.
   *                        Can be set lower to limit response length.
   * 
   * @param stopSequences List of character sequences that will stop output generation.
   *                      The model will stop at the first appearance of any sequence in this list.
   *                      The stop sequence will not be included in the response.
   *                      Optional, defaults to None.
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