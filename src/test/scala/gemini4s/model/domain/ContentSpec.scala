package gemini4s.model.domain

import munit.FunSuite

class ContentSpec extends FunSuite {

  test("Content should create with role") {
    val content = Content(parts = List(ContentPart.Text("test")), role = Some("user"))
    assertEquals(content.role, Some("user"))
    assertEquals(content.parts.length, 1)
  }

  test("Content should create without role") {
    val content = Content(parts = List(ContentPart.Text("test")))
    assertEquals(content.role, None)
  }

  test("Content should handle multiple parts") {
    val content = Content(parts = List(ContentPart.Text("first"), ContentPart.Text("second")))
    assertEquals(content.parts.length, 2)
  }

  test("Content should handle empty parts list") {
    val content = Content(parts = List())
    assertEquals(content.parts, List())
  }

  test("ContentPart.Base64Data should wrap string") {
    val data = ContentPart.Base64Data("data")
    assertEquals(data.value, "data")
  }

  test("ContentPart.FileUri should wrap string") {
    val uri = ContentPart.FileUri("gs://bucket/file")
    assertEquals(uri.value, "gs://bucket/file")
  }
}
