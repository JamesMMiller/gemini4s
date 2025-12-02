package gemini4s.model.response

import io.circe._
import io.circe.generic.semiauto._

import gemini4s.model.domain.File

final case class ListFilesResponse(
    files: Option[List[File]] = None,
    nextPageToken: Option[String] = None
)

object ListFilesResponse {
  given Encoder[ListFilesResponse] = deriveEncoder
  given Decoder[ListFilesResponse] = deriveDecoder
}
