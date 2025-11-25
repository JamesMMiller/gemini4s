package gemini4s.model.response

import io.circe._
import io.circe.generic.semiauto._

/**
 * Response from creating cached content.
 */
final case class CachedContent(
    name: String,
    model: String,
    createTime: String,
    updateTime: String,
    expireTime: String,
    displayName: Option[String] = None
)

object CachedContent {
  given Decoder[CachedContent] = deriveDecoder
  given Encoder[CachedContent] = deriveEncoder
}
