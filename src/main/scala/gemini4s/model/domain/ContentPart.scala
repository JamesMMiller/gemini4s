package gemini4s.model.domain

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

/**
 * A part of the content.
 */
sealed trait ContentPart

object ContentPart {

  opaque type MimeType = String

  object MimeType {
    def apply(value: String): MimeType        = value
    extension (a: MimeType) def value: String = a
    given Encoder[MimeType]                   = Encoder.encodeString
    given Decoder[MimeType]                   = Decoder.decodeString
  }

  opaque type Base64Data = String

  object Base64Data {
    def apply(value: String): Base64Data        = value
    extension (a: Base64Data) def value: String = a
    given Encoder[Base64Data]                   = Encoder.encodeString
    given Decoder[Base64Data]                   = Decoder.decodeString
  }

  opaque type FileUri = String

  object FileUri {
    def apply(value: String): FileUri        = value
    extension (a: FileUri) def value: String = a
    given Encoder[FileUri]                   = Encoder.encodeString
    given Decoder[FileUri]                   = Decoder.decodeString
  }

  final case class Text(text: String)                               extends ContentPart
  final case class InlineData(mimeType: MimeType, data: Base64Data) extends ContentPart
  final case class FileData(mimeType: MimeType, fileUri: FileUri)   extends ContentPart

  object InlineData {
    given Encoder[InlineData] = deriveEncoder
    given Decoder[InlineData] = deriveDecoder
  }

  object FileData {
    given Encoder[FileData] = deriveEncoder
    given Decoder[FileData] = deriveDecoder
  }

  given Encoder[ContentPart] = Encoder.instance {
    case Text(text)    => Json.obj("text" -> text.asJson)
    case i: InlineData => Json.obj("inlineData" -> i.asJson)
    case f: FileData   => Json.obj("fileData" -> f.asJson)
  }

  given Decoder[ContentPart] = Decoder.instance { cursor =>
    cursor
      .downField("text")
      .as[String]
      .map(Text.apply)
      .orElse(cursor.downField("inlineData").as[InlineData])
      .orElse(cursor.downField("fileData").as[FileData])
  }

}
