package gemini4s.model.domain

import io.circe._

opaque type GcsUri = String

object GcsUri {
  def apply(value: String): GcsUri = value

  extension (a: GcsUri) {
    def value: String = a
  }

  given Encoder[GcsUri] = Encoder.encodeString
  given Decoder[GcsUri] = Decoder.decodeString
}
