package gemini4s.model.request

import io.circe._
import io.circe.generic.semiauto._
import gemini4s.model.domain._

/**
 * Request for text generation using the Gemini API.
 */
final case class GenerateContentRequest(
    model: String,
    contents: List[Content],
    safetySettings: Option[List[SafetySetting]] = None,
    generationConfig: Option[GenerationConfig] = None,
    systemInstruction: Option[Content] = None,
    tools: Option[List[Tool]] = None,
    toolConfig: Option[ToolConfig] = None
)

object GenerateContentRequest {
  given Encoder[GenerateContentRequest] = deriveEncoder
  given Decoder[GenerateContentRequest] = deriveDecoder
}
