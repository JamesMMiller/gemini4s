package gemini4s.model

import zio.json._
import zio.test.Assertion._
import zio.test._

import gemini4s.model.GeminiCodecs.given
import gemini4s.model.GeminiResponse._

object GeminiResponseSpec extends ZIOSpecDefault {
  def spec = suite("GeminiResponse")(
    test("GenerateContentResponse should serialize and deserialize correctly") {
      val expected = GenerateContentResponse(
        candidates = List(
          Candidate(
            content = ResponseContent(
              parts = List(Part(text = "Generated text")),
              role = Some("model")
            ),
            finishReason = Some("STOP"),
            safetyRatings = Some(List(
              SafetyRating(
                category = "HARASSMENT",
                probability = "LOW"
              )
            ))
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

      val json = expected.toJson
      val result = json.fromJson[GenerateContentResponse]

      assertTrue(
        result.isRight,
        result.exists(_.candidates.head.content.parts.head == Part(text = "Generated text")),
        result.exists(_.candidates.head.finishReason == Some("STOP")),
        result.exists(_.candidates.head.safetyRatings.exists(_.head.category == "HARASSMENT")),
        result.exists(_.candidates.head.safetyRatings.exists(_.head.probability == "LOW")),
        result.exists(_.usageMetadata.exists(_.promptTokenCount == 10)),
        result.exists(_.usageMetadata.exists(_.candidatesTokenCount == 20)),
        result.exists(_.usageMetadata.exists(_.totalTokenCount == 30)),
        result.exists(_.modelVersion.contains("gemini-pro"))
      )
    },

    test("ResponseContent should serialize and deserialize correctly") {
      val expected = ResponseContent(
        parts = List(Part(text = "Test text")),
        role = Some("model")
      )

      val json = expected.toJson
      val result = json.fromJson[ResponseContent]

      assertTrue(
        result.isRight,
        result.exists(_.parts == List(Part(text = "Test text"))),
        result.exists(_.role.contains("model"))
      )
    },

    test("SafetyRating should serialize and deserialize correctly") {
      val expected = SafetyRating(
        category = "SEXUALLY_EXPLICIT",
        probability = "MEDIUM"
      )

      val json = expected.toJson
      val result = json.fromJson[SafetyRating]

      assertTrue(
        result.isRight,
        result.exists(_.category == "SEXUALLY_EXPLICIT"),
        result.exists(_.probability == "MEDIUM")
      )
    },

    test("UsageMetadata should serialize and deserialize correctly") {
      val expected = UsageMetadata(
        promptTokenCount = 10,
        candidatesTokenCount = 20,
        totalTokenCount = 30
      )

      val json = expected.toJson
      val result = json.fromJson[UsageMetadata]

      assertTrue(
        result.isRight,
        result.exists(_.promptTokenCount == 10),
        result.exists(_.candidatesTokenCount == 20),
        result.exists(_.totalTokenCount == 30)
      )
    }
  )
} 