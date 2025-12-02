package gemini4s.model.domain

import io.circe._
import io.circe.generic.semiauto._

/**
 * Configuration for code execution.
 */
final case class CodeExecution()

object CodeExecution {
  given Encoder[CodeExecution] = deriveEncoder
  given Decoder[CodeExecution] = deriveDecoder
}
