package gemini4s.model.domain

import io.circe._
import io.circe.generic.semiauto._

/**
 * Content part that can be used in requests.
 */
final case class Content(
    parts: List[ContentPart],
    role: Option[String] = None
)

object Content {
  given Encoder[Content] = deriveEncoder
  given Decoder[Content] = deriveDecoder
}
