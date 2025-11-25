package gemini4s.model.request

import io.circe._
import io.circe.generic.semiauto._

import gemini4s.model.domain._

/**
 * Request to count tokens for given content.
 */
final case class CountTokensRequest(
    model: ModelName,
    contents: List[Content]
)

object CountTokensRequest {
  given Encoder[CountTokensRequest] = deriveEncoder
  given Decoder[CountTokensRequest] = deriveDecoder
}
