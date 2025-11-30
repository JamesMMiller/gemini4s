package gemini4s

import munit.FunSuite
import io.circe.syntax._
import gemini4s.model.domain._

class MultimodalSpec extends FunSuite {

  test("ContentPart.InlineData should encode correctly") {
    val part: ContentPart = ContentPart.InlineData("image/jpeg", "base64data")
    val json              = part.asJson.noSpaces
    assert(json.contains("inlineData"))
    assert(json.contains("mimeType"))
    assert(json.contains("image/jpeg"))
    assert(json.contains("data"))
    assert(json.contains("base64data"))
  }

  test("ContentPart.FileData should encode correctly") {
    val part: ContentPart = ContentPart.FileData("image/png", "uri")
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
        assertEquals(mimeType, "image/png")
        assertEquals(data, "base64")
      case _                                      => fail("Expected InlineData")
    }
  }

  test("GeminiService.file helper should create FileData") {
    val content = GeminiService.file("uri", "application/pdf")
    assertEquals(content.parts.length, 1)
    content.parts.head match {
      case ContentPart.FileData(mimeType, fileUri) =>
        assertEquals(mimeType, "application/pdf")
        assertEquals(fileUri, "uri")
      case _                                       => fail("Expected FileData")
    }
  }
}
