package gemini4s.model

import gemini4s.model.GeminiRequest.{HarmBlockThreshold, HarmCategory}

/**
 * Base trait for all Gemini API responses.
 */
sealed trait GeminiResponse

object GeminiResponse {
  /**
   * Response from content generation.
   *
   * @param candidates List of generated candidates
   * @param promptFeedback Feedback about the prompt
   */
  final case class GenerateContentResponse(
    candidates: List[Candidate],
    promptFeedback: Option[PromptFeedback]
  ) extends GeminiResponse

  /**
   * A generated candidate response.
   *
   * @param content The generated content
   * @param finishReason Why the generation stopped
   * @param safetyRatings Safety ratings for the content
   * @param citationMetadata Citations for the content
   */
  final case class Candidate(
    content: ResponseContent,
    finishReason: FinishReason,
    safetyRatings: List[SafetyRating],
    citationMetadata: Option[CitationMetadata]
  )

  /**
   * Content in a response.
   *
   * @param parts The parts making up the content
   * @param role The role of the content (e.g., user, assistant)
   */
  final case class ResponseContent(
    parts: List[ResponsePart],
    role: Option[String]
  )

  /**
   * A part of content in a response.
   */
  sealed trait ResponsePart
  object ResponsePart {
    final case class Text(text: String) extends ResponsePart
  }

  /**
   * Why the generation finished.
   */
  enum FinishReason {
    case FINISH_REASON_UNSPECIFIED
    case STOP
    case MAX_TOKENS
    case SAFETY
    case RECITATION
    case OTHER
  }

  /**
   * Safety rating for generated content.
   *
   * @param category The harm category
   * @param probability The probability of harm
   */
  final case class SafetyRating(
    category: HarmCategory,
    probability: HarmProbability
  )

  /**
   * Probability levels for harmful content.
   */
  enum HarmProbability {
    case HARM_PROBABILITY_UNSPECIFIED
    case NEGLIGIBLE
    case LOW
    case MEDIUM
    case HIGH
  }

  /**
   * Metadata for content citations.
   *
   * @param citations List of citations
   */
  final case class CitationMetadata(
    citations: List[Citation]
  )

  /**
   * A citation for generated content.
   *
   * @param startIndex Start of cited content
   * @param endIndex End of cited content
   * @param url Source URL
   * @param title Source title
   * @param license License information
   */
  final case class Citation(
    startIndex: Option[Int],
    endIndex: Option[Int],
    url: Option[String],
    title: Option[String],
    license: Option[String]
  )

  /**
   * Feedback about the prompt.
   *
   * @param blockReason Why the prompt was blocked
   * @param safetyRatings Safety ratings for the prompt
   */
  final case class PromptFeedback(
    blockReason: Option[BlockReason],
    safetyRatings: List[SafetyRating]
  )

  /**
   * Reasons why a prompt might be blocked.
   */
  enum BlockReason {
    case BLOCK_REASON_UNSPECIFIED
    case SAFETY
    case OTHER
  }
} 