package gemini4s.model.domain

import io.circe._
import io.circe.generic.semiauto._

/**
 * Configuration for text generation.
 */
final case class GenerationConfig(
    temperature: Option[Temperature] = None,
    topK: Option[TopK] = None,
    topP: Option[TopP] = None,
    candidateCount: Option[Int] = None,
    maxOutputTokens: Option[Int] = None,
    stopSequences: Option[List[String]] = None,
    responseMimeType: Option[MimeType] = None,
    responseSchema: Option[Schema] = None
)

object GenerationConfig {
  given Encoder[GenerationConfig] = deriveEncoder
  given Decoder[GenerationConfig] = deriveDecoder
}
