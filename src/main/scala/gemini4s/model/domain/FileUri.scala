package gemini4s.model.domain

import io.circe._

opaque type FileUri = String

object FileUri {
  def apply(value: String): FileUri        = value
  extension (a: FileUri) def value: String = a
  given Encoder[FileUri]                   = Encoder.encodeString
  given Decoder[FileUri]                   = Decoder.decodeString
}
