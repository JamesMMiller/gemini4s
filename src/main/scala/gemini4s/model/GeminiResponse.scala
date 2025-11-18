package gemini4s.model

import io.circe._
import io.circe.generic.semiauto._

object GeminiResponse {

  /**
   * Response from content generation.
   */
  final case class GenerateContentResponse(
      candidates: List[Candidate],
      usageMetadata: Option[UsageMetadata] = None,
      modelVersion: Option[String] = None
  )

  object GenerateContentResponse {
    given Decoder[GenerateContentResponse] = deriveDecoder
    given Encoder[GenerateContentResponse] = deriveEncoder
  }

  /**
   * Response from token counting.
   */
  final case class CountTokensResponse(
      totalTokens: Int
  )

  object CountTokensResponse {
    given Decoder[CountTokensResponse] = deriveDecoder
    given Encoder[CountTokensResponse] = deriveEncoder
  }

  /**
   * A candidate response from the model.
   */
  final case class Candidate(
      content: ResponseContent,
      finishReason: Option[String] = None,
      index: Option[Int] = None,
      safetyRatings: Option[List[SafetyRating]] = None
  )

  object Candidate {
    given Decoder[Candidate] = deriveDecoder
    given Encoder[Candidate] = deriveEncoder
  }

  /**
   * Content part in a response.
   */
  final case class ResponseContent(
      parts: List[ResponsePart],
      role: Option[String] = None
  )

  object ResponseContent {
    given Decoder[ResponseContent] = deriveDecoder
    given Encoder[ResponseContent] = deriveEncoder
  }

  /**
   * A part of the response content.
   */
  final case class ResponsePart(
      text: String
  )

  object ResponsePart {
    given Decoder[ResponsePart] = deriveDecoder
    given Encoder[ResponsePart] = deriveEncoder
  }

  /**
   * Safety rating for generated content.
   */
  final case class SafetyRating(
      category: String,
      probability: String
  )

  object SafetyRating {
    given Decoder[SafetyRating] = deriveDecoder
    given Encoder[SafetyRating] = deriveEncoder
  }

  /**
   * Usage metadata for the request.
   */
  final case class UsageMetadata(
      promptTokenCount: Int,
      candidatesTokenCount: Int,
      totalTokenCount: Int
  )

  object UsageMetadata {
    given Decoder[UsageMetadata] = deriveDecoder
    given Encoder[UsageMetadata] = deriveEncoder
  }

}
