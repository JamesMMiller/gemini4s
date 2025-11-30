package gemini4s.model.domain

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

/**
 * A part of the content.
 */
sealed trait ContentPart

object ContentPart {
  final case class Text(text: String)                          extends ContentPart
  final case class InlineData(mimeType: String, data: String)  extends ContentPart
  final case class FileData(mimeType: String, fileUri: String) extends ContentPart

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
