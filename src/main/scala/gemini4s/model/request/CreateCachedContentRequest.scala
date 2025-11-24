package gemini4s.model.request

import io.circe._
import io.circe.generic.semiauto._
import gemini4s.model.domain._

/**
 * Request to create cached content.
 */
final case class CreateCachedContentRequest(
    model: Option[String] = None,
    systemInstruction: Option[Content] = None,
    contents: Option[List[Content]] = None,
    tools: Option[List[Tool]] = None,
    toolConfig: Option[ToolConfig] = None,
    ttl: Option[String] = None,
    displayName: Option[String] = None
)

object CreateCachedContentRequest {
  given Encoder[CreateCachedContentRequest] = deriveEncoder
  given Decoder[CreateCachedContentRequest] = deriveDecoder
}
