package gemini4s.model.request

import io.circe._
import io.circe.generic.semiauto._
import gemini4s.model.domain._

/**
 * Request to embed content.
 */
final case class EmbedContentRequest(
    content: Content,
    model: String,
    taskType: Option[TaskType] = None,
    title: Option[String] = None,
    outputDimensionality: Option[Int] = None
)

object EmbedContentRequest {
  given Encoder[EmbedContentRequest] = deriveEncoder
  given Decoder[EmbedContentRequest] = deriveDecoder
}
