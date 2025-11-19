package gemini4s.model

import io.circe._
import io.circe.generic.semiauto._

/**
 * Base trait for all Gemini API requests.
 */
trait GeminiRequest

object GeminiRequest {

  given Encoder[GeminiRequest] = Encoder.instance {
    case req: GenerateContent    => deriveEncoder[GenerateContent].apply(req)
    case req: CountTokensRequest => deriveEncoder[CountTokensRequest].apply(req)
  }

  /**
   * Request for text generation using the Gemini API.
   */
  final case class GenerateContent(
      contents: List[Content],
      safetySettings: Option[List[SafetySetting]] = None,
      generationConfig: Option[GenerationConfig] = None
  ) extends GeminiRequest

  object GenerateContent {
    given Encoder[GenerateContent] = deriveEncoder
    given Decoder[GenerateContent] = deriveDecoder
  }

  /**
   * Request to count tokens for given content.
   */
  final case class CountTokensRequest(
      contents: List[Content]
  ) extends GeminiRequest

  object CountTokensRequest {
    given Encoder[CountTokensRequest] = deriveEncoder
    given Decoder[CountTokensRequest] = deriveDecoder
  }

  /**
   * Content part that can be used in requests.
   */
  final case class Content(
      parts: List[Part],
      role: Option[String] = None
  )

  object Content {
    given Encoder[Content] = deriveEncoder
    given Decoder[Content] = deriveDecoder
  }

  /**
   * A part of the content.
   */
  final case class Part(text: String)

  object Part {
    given Encoder[Part] = deriveEncoder
    given Decoder[Part] = deriveDecoder
  }

  /**
   * Safety setting for content filtering.
   */
  final case class SafetySetting(
      category: HarmCategory,
      threshold: HarmBlockThreshold
  )

  object SafetySetting {
    given Encoder[SafetySetting] = deriveEncoder
    given Decoder[SafetySetting] = deriveDecoder
  }

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

  object HarmCategory {
    given Encoder[HarmCategory] = Encoder.encodeString.contramap(_.toString)

    given Decoder[HarmCategory] = Decoder.decodeString.emap { str =>
      try Right(HarmCategory.valueOf(str))
      catch { case _: IllegalArgumentException => Left(s"Unknown HarmCategory: $str") }
    }

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

  object HarmBlockThreshold {
    given Encoder[HarmBlockThreshold] = Encoder.encodeString.contramap(_.toString)

    given Decoder[HarmBlockThreshold] = Decoder.decodeString.emap { str =>
      try Right(HarmBlockThreshold.valueOf(str))
      catch { case _: IllegalArgumentException => Left(s"Unknown HarmBlockThreshold: $str") }
    }

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

  object GenerationConfig {
    given Encoder[GenerationConfig] = deriveEncoder
    given Decoder[GenerationConfig] = deriveDecoder
  }

}
