package gemini4s.model

import zio.json._

object GeminiResponse {
  /**
   * Response from content generation.
   */
  final case class GenerateContentResponse(
    candidates: List[Candidate],
    usageMetadata: Option[UsageMetadata] = None,
    modelVersion: Option[String] = None
  )

  /**
   * Response from token counting.
   */
  final case class CountTokensResponse(
    tokenCount: Int
  )

  /**
   * A candidate response from the model.
   */
  final case class Candidate(
    content: ResponseContent,
    finishReason: Option[String] = None,
    index: Option[Int] = None,
    safetyRatings: Option[List[SafetyRating]] = None
  )

  /**
   * Content part in a response.
   */
  final case class ResponseContent(
    parts: List[ResponsePart],
    role: Option[String] = None
  )

  /**
   * A part of the response content.
   */
  final case class ResponsePart(
    text: String
  )

  /**
   * Safety rating for generated content.
   */
  final case class SafetyRating(
    category: String,
    probability: String
  )

  /**
   * Usage metadata for the request.
   */
  final case class UsageMetadata(
    promptTokenCount: Int,
    candidatesTokenCount: Int,
    totalTokenCount: Int
  )
} 