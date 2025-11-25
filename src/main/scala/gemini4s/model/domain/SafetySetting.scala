package gemini4s.model.domain

import io.circe._
import io.circe.generic.semiauto._

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
