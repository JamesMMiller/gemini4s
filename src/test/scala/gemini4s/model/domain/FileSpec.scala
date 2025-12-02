package gemini4s.model.domain

import io.circe.parser._
import io.circe.syntax._
import munit.FunSuite

import gemini4s.model.response.ListFilesResponse

class FileSpec extends FunSuite {

  test("File model should encode/decode correctly") {
    val file = File(
      name = "files/123",
      displayName = Some("test.txt"),
      mimeType = Some(MimeType.unsafe("text/plain")),
      sizeBytes = Some(SizeBytes(100L)),
      createTime = Some("2024-01-01T00:00:00Z"),
      updateTime = Some("2024-01-01T00:00:00Z"),
      expirationTime = Some("2024-01-02T00:00:00Z"),
      sha256Hash = Some("hash"),
      uri = FileUri("http://file-uri"),
      state = Some(FileState.ACTIVE),
      error = None
    )

    val json = file.asJson
    assertEquals(json.as[File], Right(file))
  }

  test("FileState should encode/decode correctly") {
    assertEquals(FileState.ACTIVE.asJson.asString, Some("ACTIVE"))
    assertEquals(decode[FileState]("\"ACTIVE\""), Right(FileState.ACTIVE))
    assertEquals(decode[FileState]("\"PROCESSING\""), Right(FileState.PROCESSING))
    assertEquals(decode[FileState]("\"FAILED\""), Right(FileState.FAILED))
    assertEquals(decode[FileState]("\"STATE_UNSPECIFIED\""), Right(FileState.STATE_UNSPECIFIED))
    assert(decode[FileState]("\"UNKNOWN\"").isLeft)
  }

  test("ListFilesResponse should encode/decode correctly") {
    val file     = File(
      name = "files/123",
      uri = FileUri("http://file-uri")
    )
    val response = ListFilesResponse(
      files = Some(List(file)),
      nextPageToken = Some("next-page")
    )

    val json = response.asJson
    assertEquals(json.as[ListFilesResponse], Right(response))
  }
}
