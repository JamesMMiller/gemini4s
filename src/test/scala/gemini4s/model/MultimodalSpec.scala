package gemini4s.model

import io.circe.syntax._
import munit.FunSuite

import gemini4s.GeminiService
import gemini4s.model.domain._

class MultimodalSpec extends FunSuite {

  test("ContentPart.InlineData should encode correctly") {
    val part: ContentPart = ContentPart.InlineData(MimeType.unsafe("image/jpeg"), ContentPart.Base64Data("base64data"))
    val json              = part.asJson
    assertEquals(json.as[ContentPart], Right(part))
  }

  test("ContentPart.FileData should encode correctly") {
    val part: ContentPart = ContentPart.FileData(MimeType.unsafe("image/png"), ContentPart.FileUri("uri"))
    val json              = part.asJson.noSpaces
    assert(json.contains("fileData"))
    assert(json.contains("mimeType"))
    assert(json.contains("image/png"))
    assert(json.contains("fileUri"))
    assert(json.contains("uri"))
  }

  test("GeminiService.image helper should create InlineData") {
    val content = GeminiService.image("base64", "image/png")
    assertEquals(content.parts.length, 1)
    content.parts.head match {
      case ContentPart.InlineData(mimeType, data) =>
        assertEquals(mimeType.value, "image/png")
        assertEquals(data.value, "base64")
      case _                                      => fail("Expected InlineData")
    }
  }

  test("GeminiService.file helper should create FileData") {
    val content = GeminiService.file("uri", "application/pdf")
    assertEquals(content.parts.length, 1)
    content.parts.head match {
      case ContentPart.FileData(mimeType, fileUri) =>
        assertEquals(mimeType.value, "application/pdf")
        assertEquals(fileUri.value, "uri")
      case _                                       => fail("Expected FileData")
    }
  }

  test("MimeType codec") {
    val mimeType = MimeType.unsafe("application/json")
    val json     = mimeType.asJson
    assertEquals(json.asString, Some("application/json"))
    assertEquals(json.as[MimeType], Right(mimeType))
  }

  test("Base64Data codec should work") {
    val data = ContentPart.Base64Data("base64")
    val json = data.asJson
    assertEquals(json.asString, Some("base64"))
    assertEquals(json.as[ContentPart.Base64Data], Right(data))
  }

  test("FileUri codec should work") {
    val uri  = ContentPart.FileUri("http://example.com")
    val json = uri.asJson
    assertEquals(json.asString, Some("http://example.com"))
    assertEquals(json.as[ContentPart.FileUri], Right(uri))
  }

  test("GeminiService.image helper should create correct ContentPart") {
    val base64   = "base64data"
    val mimeType = "image/png"
    val content  = GeminiService.image(base64, mimeType)
    content.parts.head match {
      case ContentPart.InlineData(m, d) =>
        assertEquals(m.value, mimeType)
        assertEquals(d.value, base64)
      case _                            => fail("Expected InlineData")
    }
  }

  test("GeminiService.file helper should create correct ContentPart") {
    val uri      = "http://example.com"
    val mimeType = "application/pdf"
    val content  = GeminiService.file(uri, mimeType)
    content.parts.head match {
      case ContentPart.FileData(m, u) =>
        assertEquals(m.value, mimeType)
        assertEquals(u.value, uri)
      case _                          => fail("Expected FileData")
    }
  }
}
