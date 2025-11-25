package gemini4s.model.request

import io.circe._
import io.circe.generic.semiauto._

import gemini4s.model.domain._

/**
 * Request to batch embed contents.
 */
final case class BatchEmbedContentsRequest(
    model: ModelName,
    requests: List[EmbedContentRequest]
)

object BatchEmbedContentsRequest {
  given Encoder[BatchEmbedContentsRequest] = deriveEncoder
  given Decoder[BatchEmbedContentsRequest] = deriveDecoder
}
