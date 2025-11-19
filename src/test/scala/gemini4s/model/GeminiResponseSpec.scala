package gemini4s.model

import io.circe.parser._
import io.circe.syntax._
import munit.FunSuite

import gemini4s.model.GeminiResponse._

class GeminiResponseSpec extends FunSuite {

  test("GenerateContentResponse should serialize and deserialize correctly") {
    val expected = GenerateContentResponse(
      candidates = List(
        Candidate(
          content = ResponseContent(
            parts = List(ResponsePart(text = "Generated text")),
            role = Some("model")
          ),
          finishReason = Some("STOP"),
          safetyRatings = Some(
            List(
              SafetyRating(
                category = "HARASSMENT",
                probability = "LOW"
              )
            )
          )
        )
      ),
      usageMetadata = Some(
        UsageMetadata(
          promptTokenCount = 10,
          candidatesTokenCount = 20,
          totalTokenCount = 30
        )
      ),
      modelVersion = Some("gemini-pro")
    )

    val json   = expected.asJson.noSpaces
    val result = decode[GenerateContentResponse](json)

    assert(result.isRight)
    val decoded = result.toOption.get
    assertEquals(decoded.candidates.head.content.parts.head, ResponsePart(text = "Generated text"))
    assertEquals(decoded.candidates.head.finishReason, Some("STOP"))
    assertEquals(decoded.candidates.head.safetyRatings.flatMap(_.headOption.map(_.category)), Some("HARASSMENT"))
    assertEquals(decoded.candidates.head.safetyRatings.flatMap(_.headOption.map(_.probability)), Some("LOW"))
    assertEquals(decoded.usageMetadata.map(_.promptTokenCount), Some(10))
    assertEquals(decoded.usageMetadata.map(_.candidatesTokenCount), Some(20))
    assertEquals(decoded.usageMetadata.map(_.totalTokenCount), Some(30))
    assertEquals(decoded.modelVersion, Some("gemini-pro"))
  }

  test("ResponseContent should serialize and deserialize correctly") {
    val expected = ResponseContent(
      parts = List(ResponsePart(text = "Test text")),
      role = Some("model")
    )

    val json   = expected.asJson.noSpaces
    val result = decode[ResponseContent](json)

    assert(result.isRight)
    val decoded = result.toOption.get
    assertEquals(decoded.parts, List(ResponsePart(text = "Test text")))
    assertEquals(decoded.role, Some("model"))
  }

  test("SafetyRating should serialize and deserialize correctly") {
    val expected = SafetyRating(
      category = "SEXUALLY_EXPLICIT",
      probability = "MEDIUM"
    )

    val json   = expected.asJson.noSpaces
    val result = decode[SafetyRating](json)

    assert(result.isRight)
    val decoded = result.toOption.get
    assertEquals(decoded.category, "SEXUALLY_EXPLICIT")
    assertEquals(decoded.probability, "MEDIUM")
  }

  test("UsageMetadata should serialize and deserialize correctly") {
    val expected = UsageMetadata(
      promptTokenCount = 10,
      candidatesTokenCount = 20,
      totalTokenCount = 30
    )

    val json   = expected.asJson.noSpaces
    val result = decode[UsageMetadata](json)

    assert(result.isRight)
    val decoded = result.toOption.get
    assertEquals(decoded.promptTokenCount, 10)
    assertEquals(decoded.candidatesTokenCount, 20)
    assertEquals(decoded.totalTokenCount, 30)
  }
}
