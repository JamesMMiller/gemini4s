package gemini4s.model.response

import io.circe._
import io.circe.generic.semiauto._

/**
 * Response from batch embedding contents.
 */
final case class BatchEmbedContentsResponse(
    embeddings: List[ContentEmbedding]
)

object BatchEmbedContentsResponse {
  given Decoder[BatchEmbedContentsResponse] = deriveDecoder
  given Encoder[BatchEmbedContentsResponse] = deriveEncoder
}
