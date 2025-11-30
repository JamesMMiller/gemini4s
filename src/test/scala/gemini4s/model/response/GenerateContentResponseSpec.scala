package gemini4s.model.response

import io.circe.parser._
import io.circe.syntax._
import munit.FunSuite

class GenerateContentResponseSpec extends FunSuite {

  test("GenerateContentResponse should decode with optional fields") {
    val json   = """{"candidates":[{"content":{"parts":[{"text":"Hello"}]}}]}"""
    val result = decode[GenerateContentResponse](json)
    assert(result.isRight)
  }

  test("GenerateContentResponse should handle usageMetadata") {
    val response = GenerateContentResponse(
      candidates = List(),
      usageMetadata = Some(UsageMetadata(Some(10), Some(20), 30))
    )
    assert(response.usageMetadata.isDefined)
    assertEquals(response.usageMetadata.get.totalTokenCount, 30)
  }

  test("GenerateContentResponse should handle modelVersion") {
    val response = GenerateContentResponse(
      candidates = List(),
      modelVersion = Some("gemini-2.0")
    )
    assertEquals(response.modelVersion, Some("gemini-2.0"))
  }

  test("GenerateContentResponse should handle promptFeedback") {
    val feedback = PromptFeedback(blockReason = Some("SAFETY"))
    val response = GenerateContentResponse(
      candidates = List(),
      promptFeedback = Some(feedback)
    )
    assert(response.promptFeedback.isDefined)
  }

  test("Candidate should handle all fields") {
    val candidate = Candidate(
      content = Some(ResponseContent(List(ResponsePart.Text("test")))),
      finishReason = Some("STOP"),
      index = Some(0),
      safetyRatings = Some(List(SafetyRating("cat", "prob")))
    )
    assertEquals(candidate.finishReason, Some("STOP"))
    assertEquals(candidate.index, Some(0))
  }

  test("ResponsePart.Text should encode/decode") {
    val part: ResponsePart = ResponsePart.Text("Hello")
    val json               = summon[io.circe.Encoder[ResponsePart]].apply(part)
    val decoded            = json.as[ResponsePart]
    assert(decoded.isRight)
  }

  test("ResponsePart.FunctionCall should encode/decode") {
    val functionCall       = FunctionCallData("getName", Map("arg" -> io.circe.Json.fromString("value")))
    val part: ResponsePart = ResponsePart.FunctionCall(functionCall)
    val json               = summon[io.circe.Encoder[ResponsePart]].apply(part)
    val decoded            = json.as[ResponsePart]
    assert(decoded.isRight)
  }

  test("UsageMetadata should handle optional fields") {
    val metadata = UsageMetadata(Some(10), Some(20), 30)
    assertEquals(metadata.promptTokenCount, Some(10))
    assertEquals(metadata.candidatesTokenCount, Some(20))
    assertEquals(metadata.totalTokenCount, 30)
  }

  test("UsageMetadata should handle missing optional fields") {
    val metadata = UsageMetadata(None, None, 30)
    assertEquals(metadata.promptTokenCount, None)
    assertEquals(metadata.candidatesTokenCount, None)
  }
}
