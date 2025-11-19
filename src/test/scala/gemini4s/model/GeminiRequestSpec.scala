package gemini4s.model

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import munit.FunSuite

import gemini4s.model.GeminiRequest._

class GeminiRequestSpec extends FunSuite {

  test("Content should serialize to JSON correctly") {
    val expected = Content(parts = List(Part.Text(text = "Test prompt")))
    val json     = expected.asJson.noSpaces
    val parsed   = decode[Content](json)

    val expectedJson = parse("""{"parts":[{"text":"Test prompt"}],"role":null}""").getOrElse(Json.Null)
    assertEquals(parse(json), Right(expectedJson))
    assertEquals(parsed, Right(expected))
  }

  test("SafetySetting should serialize to JSON correctly") {
    val expected = SafetySetting(
      category = HarmCategory.HARASSMENT,
      threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
    )
    val json     = expected.asJson.noSpaces
    val parsed   = decode[SafetySetting](json)

    val expectedJson = parse("""{"category":"HARASSMENT","threshold":"BLOCK_MEDIUM_AND_ABOVE"}""").getOrElse(Json.Null)
    assertEquals(parse(json), Right(expectedJson))
    assertEquals(parsed, Right(expected))
  }

  test("GenerationConfig should serialize to JSON correctly") {
    val expected = GenerationConfig(
      temperature = Some(0.8f),
      candidateCount = Some(2),
      maxOutputTokens = Some(100),
      topP = Some(0.9f),
      topK = Some(10),
      stopSequences = Some(List(".", "!"))
    )
    val json     = expected.asJson.noSpaces
    val parsed   = decode[GenerationConfig](json)

    val expectedJson = parse(
      """{"temperature":0.8,"topK":10,"topP":0.9,"candidateCount":2,"maxOutputTokens":100,"stopSequences":[".","!"]}"""
    ).getOrElse(Json.Null)
    assertEquals(parse(json), Right(expectedJson))
    assertEquals(parsed, Right(expected))
  }

  test("GenerateContent should serialize to JSON correctly") {
    val expected = GenerateContent(
      contents = List(Content(parts = List(Part.Text(text = "Test prompt")))),
      safetySettings = Some(
        List(
          SafetySetting(
            category = HarmCategory.HARASSMENT,
            threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
          )
        )
      ),
      generationConfig = Some(
        GenerationConfig(
          temperature = Some(0.8f),
          candidateCount = Some(2),
          maxOutputTokens = Some(100),
          topP = Some(0.9f),
          topK = Some(10),
          stopSequences = Some(List(".", "!"))
        )
      )
    )
    val json     = expected.asJson.noSpaces
    val parsed   = decode[GenerateContent](json)

    val expectedJson = parse(
      "{\"contents\":[{\"parts\":[{\"text\":\"Test prompt\"}],\"role\":null}],\"safetySettings\":[{\"category\":\"HARASSMENT\",\"threshold\":\"BLOCK_MEDIUM_AND_ABOVE\"}],\"generationConfig\":{\"temperature\":0.8,\"topK\":10,\"topP\":0.9,\"candidateCount\":2,\"maxOutputTokens\":100,\"stopSequences\":[\".\",\"!\"]},\"systemInstruction\":null}"
    ).getOrElse(Json.Null)
    assertEquals(parse(json), Right(expectedJson))
    assertEquals(parsed, Right(expected))
  }
}
