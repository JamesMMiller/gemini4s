package gemini4s.model.domain

import io.circe._
import io.circe.generic.semiauto._

final case class File(
    name: String,
    displayName: Option[String] = None,
    mimeType: Option[MimeType] = None,
    sizeBytes: Option[SizeBytes] = None,
    createTime: Option[String] = None,
    updateTime: Option[String] = None,
    expirationTime: Option[String] = None,
    sha256Hash: Option[String] = None,
    uri: FileUri,
    state: Option[FileState] = None,
    error: Option[Status] = None
)

object File {
  given Encoder[File] = deriveEncoder
  given Decoder[File] = deriveDecoder
}

enum FileState {
  case STATE_UNSPECIFIED, PROCESSING, ACTIVE, FAILED
}

object FileState {
  given Encoder[FileState] = Encoder.encodeString.contramap(_.toString)

  given Decoder[FileState] = Decoder.decodeString.emap { str =>
    try Right(FileState.valueOf(str))
    catch { case _: IllegalArgumentException => Left(s"Unknown FileState: $str") }
  }

}

// Status definition for error field (simplified)
final case class Status(
    code: Int,
    message: String,
    details: Option[List[io.circe.Json]] = None
)

object Status {
  given Encoder[Status] = deriveEncoder
  given Decoder[Status] = deriveDecoder
}
