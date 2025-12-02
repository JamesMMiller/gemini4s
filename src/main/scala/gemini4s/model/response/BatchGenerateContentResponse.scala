package gemini4s.model.response

import io.circe._
import io.circe.generic.semiauto._

/**
 * Response from batch generate content.
 */
final case class BatchGenerateContentResponse(
    candidates: List[GenerateContentResponse]
)

object BatchGenerateContentResponse {
  given Encoder[BatchGenerateContentResponse] = deriveEncoder
  given Decoder[BatchGenerateContentResponse] = deriveDecoder
}
