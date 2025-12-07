package gemini4s.model.domain

import io.circe._

opaque type ResourceName = String

object ResourceName {
  def apply(value: String): ResourceName = value

  extension (a: ResourceName) {
    def value: String = a
  }

  given Encoder[ResourceName] = Encoder.encodeString
  given Decoder[ResourceName] = Decoder.decodeString
}
