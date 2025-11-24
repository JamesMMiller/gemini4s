package gemini4s.model.domain

import io.circe._
import io.circe.generic.semiauto._

/**
 * Configuration for text generation.
 */
final case class GenerationConfig(
    temperature: Option[Float] = None,
    topK: Option[Int] = None,
    topP: Option[Float] = None,
    candidateCount: Option[Int] = None,
    maxOutputTokens: Option[Int] = None,
    stopSequences: Option[List[String]] = None,
    responseMimeType: Option[String] = None
)

object GenerationConfig {
  given Encoder[GenerationConfig] = deriveEncoder
  given Decoder[GenerationConfig] = deriveDecoder
}
