package gemini4s.model.domain

import io.circe._
import io.circe.generic.semiauto._

/**
 * A part of the content.
 */
final case class ContentPart(text: String)

object ContentPart {
  given Encoder[ContentPart] = deriveEncoder
  given Decoder[ContentPart] = deriveDecoder
}
