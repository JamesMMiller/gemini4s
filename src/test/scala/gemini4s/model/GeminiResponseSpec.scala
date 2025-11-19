package gemini4s.model

import io.circe.Json
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
            parts = List(ResponsePart.Text(text = "Generated text")),
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
    assertEquals(decoded.candidates.head.content.parts.head, ResponsePart.Text(text = "Generated text"))
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
      parts = List(ResponsePart.Text(text = "Test text")),
      role = Some("model")
    )

    val json   = expected.asJson.noSpaces
    val result = decode[ResponseContent](json)

    assert(result.isRight)
    val decoded = result.toOption.get
    assertEquals(decoded.parts, List(ResponsePart.Text(text = "Test text")))
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

  test("ResponsePart should handle FunctionCall") {
    val functionCall = ResponsePart.FunctionCall(FunctionCallData(name = "get_weather", args = Map("location" -> io.circe.Json.fromString("London"))))
    val json = (functionCall: ResponsePart).asJson.noSpaces
    assert(json.contains("functionCall"))
    assert(json.contains("get_weather"))

    val decoded = decode[ResponsePart](json)
    assertEquals(decoded, Right(functionCall))
  }

  test("ResponsePart should handle ExecutableCode") {
    val code = ResponsePart.ExecutableCode(ExecutableCodeData(language = "python", code = "print('hello')"))
    val json = (code: ResponsePart).asJson.noSpaces
    assert(json.contains("executableCode"))
    assert(json.contains("python"))

    val decoded = decode[ResponsePart](json)
    assertEquals(decoded, Right(code))
  }

  test("GenerateContentResponse should handle PromptFeedback") {
    val response = GenerateContentResponse(
      candidates = List.empty,
      promptFeedback = Some(PromptFeedback(blockReason = Some("SAFETY")))
    )
    val json = response.asJson.noSpaces
    assert(json.contains("promptFeedback"))
    assert(json.contains("SAFETY"))

    val decoded = decode[GenerateContentResponse](json)
    assertEquals(decoded.map(_.promptFeedback.flatMap(_.blockReason)), Right(Some("SAFETY")))
  }

  test("ResponsePart should fail to decode invalid JSON") {
    val json = Json.obj("unknown" -> Json.fromString("value"))
    assert(json.as[ResponsePart].isLeft)
  }

  test("ResponsePart should handle CodeExecutionResult") {
    val part: ResponsePart = ResponsePart.CodeExecutionResult(
      CodeExecutionResultData(outcome = "OK", output = "Result")
    )
    val json = part.asJson
    assertEquals(json.as[ResponsePart], Right(part))
  }
}
