package gemini4s

import io.circe.Json
import io.circe.parser._
import io.circe.syntax._
import munit.FunSuite

import gemini4s.model.GeminiRequest._

class GeminiDecoderSpec extends FunSuite {

  test("Part.Text should serialize to correct JSON") {
    val part: Part = Part.Text("Hello world")
    val json       = part.asJson.noSpaces
    assertEquals(json, """{"text":"Hello world"}""")
  }

  test("Part.InlineData should serialize to correct JSON") {
    val part: Part = Part.InlineData(Part.Blob("image/jpeg", "base64data"))
    val json       = part.asJson.noSpaces
    assertEquals(json, """{"inlineData":{"mimeType":"image/jpeg","data":"base64data"}}""")
  }

  test("GenerateContent with systemInstruction should serialize correctly") {
    val request = GenerateContent(
      contents = List(Content(parts = List(Part.Text("User prompt")))),
      systemInstruction = Some(Content(parts = List(Part.Text("System instruction"))))
    )
    val json    = request.asJson

    val expectedJson = parse("""
      {
        "contents": [{"parts": [{"text": "User prompt"}], "role": null}],
        "safetySettings": null,
        "generationConfig": null,
        "systemInstruction": {"parts": [{"text": "System instruction"}], "role": null}
      }
    """).getOrElse(Json.Null)

    assertEquals(json, expectedJson)
  }

  test("GenerateContent with image should serialize correctly") {
    val request = GenerateContent(
      contents = List(
        Content(parts =
          List(
            Part.Text("Describe this image"),
            Part.InlineData(Part.Blob("image/png", "imagedata"))
          )
        )
      )
    )
    val json    = request.asJson

    val expectedJson = parse("""
      {
        "contents": [{
          "parts": [
            {"text": "Describe this image"},
            {"inlineData": {"mimeType": "image/png", "data": "imagedata"}}
          ],
          "role": null
        }],
        "safetySettings": null,
        "generationConfig": null,
        "systemInstruction": null
      }
    """).getOrElse(Json.Null)

    assertEquals(json, expectedJson)
  }
}
