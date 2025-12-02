package gemini4s.model.request

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

import gemini4s.model.domain._

/**
 * Request for text generation using the Gemini API.
 */
final case class GenerateContentRequest(
    model: ModelName,
    contents: List[Content],
    safetySettings: Option[List[SafetySetting]] = None,
    generationConfig: Option[GenerationConfig] = None,
    systemInstruction: Option[Content] = None,
    tools: Option[List[Tool]] = None,
    toolConfig: Option[ToolConfig] = None
)

object GenerateContentRequest {

  given Encoder[GenerateContentRequest] = Encoder.instance { req =>
    Json
      .obj(
        "contents"          -> req.contents.asJson,
        "safetySettings"    -> req.safetySettings.asJson,
        "generationConfig"  -> req.generationConfig.asJson,
        "systemInstruction" -> req.systemInstruction.asJson,
        "tools"             -> req.tools.asJson,
        "toolConfig"        -> req.toolConfig.asJson
      )
      .dropNullValues
  }

  given Decoder[GenerateContentRequest] = deriveDecoder
}
