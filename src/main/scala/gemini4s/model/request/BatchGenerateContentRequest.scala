package gemini4s.model.request

import io.circe._
import io.circe.generic.semiauto._

import gemini4s.model.domain._

/**
 * Request to batch generate content.
 */
final case class BatchGenerateContentRequest(
    requests: List[GenerateContentRequest]
)

object BatchGenerateContentRequest {
  given Encoder[BatchGenerateContentRequest] = deriveEncoder
  given Decoder[BatchGenerateContentRequest] = deriveDecoder
}
