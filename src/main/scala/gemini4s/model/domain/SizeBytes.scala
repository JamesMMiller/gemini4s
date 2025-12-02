package gemini4s.model.domain

import io.circe._

opaque type SizeBytes = Long

object SizeBytes {
  def apply(value: Long): SizeBytes        = value
  extension (a: SizeBytes) def value: Long = a
  given Encoder[SizeBytes]                 = Encoder.encodeLong

  // Gemini API returns sizeBytes as a string in JSON
  given Decoder[SizeBytes] = Decoder.decodeString.map(_.toLong).or(Decoder.decodeLong)
}
