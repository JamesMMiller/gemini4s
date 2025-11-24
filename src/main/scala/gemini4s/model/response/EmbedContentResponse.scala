package gemini4s.model.response

import io.circe._
import io.circe.generic.semiauto._

/**
 * Response from embedding content.
 */
final case class EmbedContentResponse(
    embedding: ContentEmbedding
)

object EmbedContentResponse {
  given Decoder[EmbedContentResponse] = deriveDecoder
  given Encoder[EmbedContentResponse] = deriveEncoder
}

/**
 * A list of floats representing an embedding.
 */
final case class ContentEmbedding(
    values: List[Float]
)

object ContentEmbedding {
  given Decoder[ContentEmbedding] = deriveDecoder
  given Encoder[ContentEmbedding] = deriveEncoder
}
