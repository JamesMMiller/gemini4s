package gemini4s.model

import zio.json._
import zio.test.Assertion._
import zio.test._

import GeminiResponse._
import GeminiRequest.{HarmCategory, HarmBlockThreshold}
import GeminiCodecs.given

object GeminiResponseSpec extends ZIOSpecDefault {
  def spec = suite("GeminiResponse")(
    test("GenerateContentResponse should serialize to JSON correctly") {
      val response = GenerateContentResponse(
        candidates = List(
          Candidate(
            content = ResponseContent(
              parts = List(ResponsePart.Text("Generated text")),
              role = Some("model")
            ),
            finishReason = FinishReason.STOP,
            safetyRatings = List(
              SafetyRating(
                category = HarmCategory.HARASSMENT,
                probability = HarmProbability.LOW
              )
            ),
            citationMetadata = Some(
              CitationMetadata(
                citations = List(
                  Citation(
                    startIndex = Some(0),
                    endIndex = Some(10),
                    url = Some("https://example.com"),
                    title = Some("Example"),
                    license = Some("MIT")
                  )
                )
              )
            )
          )
        ),
        promptFeedback = Some(
          PromptFeedback(
            blockReason = Some(BlockReason.SAFETY),
            safetyRatings = List(
              SafetyRating(
                category = HarmCategory.HATE_SPEECH,
                probability = HarmProbability.HIGH
              )
            )
          )
        )
      )

      val expectedJson = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Generated text\"}],\"role\":\"model\"},\"finishReason\":\"STOP\",\"safetyRatings\":[{\"category\":\"HARASSMENT\",\"probability\":\"LOW\"}],\"citationMetadata\":{\"citations\":[{\"startIndex\":0,\"endIndex\":10,\"url\":\"https://example.com\",\"title\":\"Example\",\"license\":\"MIT\"}]}}],\"promptFeedback\":{\"blockReason\":\"SAFETY\",\"safetyRatings\":[{\"category\":\"HATE_SPEECH\",\"probability\":\"HIGH\"}]}}"

      val actualJson = response.toJson
      assertTrue(actualJson == expectedJson)
    },

    test("GenerateContentResponse should deserialize from JSON correctly") {
      val json = """{
        |  "candidates": [
        |    {
        |      "content": {
        |        "parts": [
        |          {
        |            "text": "Generated text"
        |          }
        |        ],
        |        "role": "model"
        |      },
        |      "finishReason": "STOP",
        |      "safetyRatings": [
        |        {
        |          "category": "HARASSMENT",
        |          "probability": "LOW"
        |        }
        |      ],
        |      "citationMetadata": {
        |        "citations": [
        |          {
        |            "startIndex": 0,
        |            "endIndex": 10,
        |            "url": "https://example.com",
        |            "title": "Example",
        |            "license": "MIT"
        |          }
        |        ]
        |      }
        |    }
        |  ],
        |  "promptFeedback": {
        |    "blockReason": "SAFETY",
        |    "safetyRatings": [
        |      {
        |        "category": "HATE_SPEECH",
        |        "probability": "HIGH"
        |      }
        |    ]
        |  }
        |}""".stripMargin

      val expected = GenerateContentResponse(
        candidates = List(
          Candidate(
            content = ResponseContent(
              parts = List(ResponsePart.Text("Generated text")),
              role = Some("model")
            ),
            finishReason = FinishReason.STOP,
            safetyRatings = List(
              SafetyRating(
                category = HarmCategory.HARASSMENT,
                probability = HarmProbability.LOW
              )
            ),
            citationMetadata = Some(
              CitationMetadata(
                citations = List(
                  Citation(
                    startIndex = Some(0),
                    endIndex = Some(10),
                    url = Some("https://example.com"),
                    title = Some("Example"),
                    license = Some("MIT")
                  )
                )
              )
            )
          )
        ),
        promptFeedback = Some(
          PromptFeedback(
            blockReason = Some(BlockReason.SAFETY),
            safetyRatings = List(
              SafetyRating(
                category = HarmCategory.HATE_SPEECH,
                probability = HarmProbability.HIGH
              )
            )
          )
        )
      )

      val result = json.fromJson[GenerateContentResponse]
      assertTrue(
        result.isRight,
        result.exists(_.candidates.size == expected.candidates.size),
        result.exists(_.candidates.head.content == expected.candidates.head.content),
        result.exists(_.candidates.head.finishReason == expected.candidates.head.finishReason),
        result.exists(_.candidates.head.safetyRatings == expected.candidates.head.safetyRatings),
        result.exists(_.candidates.head.citationMetadata == expected.candidates.head.citationMetadata),
        result.exists(_.promptFeedback == expected.promptFeedback)
      )
    },

    test("ResponseContent should serialize to JSON correctly") {
      val content = ResponseContent(
        parts = List(ResponsePart.Text("Test text")),
        role = Some("assistant")
      )
      val expectedJson = """{"parts":[{"text":"Test text"}],"role":"assistant"}"""
      val actualJson = content.toJson
      assertTrue(actualJson == expectedJson)
    },

    test("ResponseContent should deserialize from JSON correctly") {
      val json = """{"parts":[{"text":"Test text"}],"role":"assistant"}"""
      val result = json.fromJson[ResponseContent]
      assertTrue(
        result.isRight,
        result.exists(_.parts == List(ResponsePart.Text("Test text"))),
        result.exists(_.role == Some("assistant"))
      )
    },

    test("SafetyRating should serialize to JSON correctly") {
      val rating = SafetyRating(
        category = HarmCategory.SEXUALLY_EXPLICIT,
        probability = HarmProbability.MEDIUM
      )
      val expectedJson = """{"category":"SEXUALLY_EXPLICIT","probability":"MEDIUM"}"""
      val actualJson = rating.toJson
      assertTrue(actualJson == expectedJson)
    },

    test("SafetyRating should deserialize from JSON correctly") {
      val json = """{"category":"SEXUALLY_EXPLICIT","probability":"MEDIUM"}"""
      val result = json.fromJson[SafetyRating]
      assertTrue(
        result.isRight,
        result.exists(_.category == HarmCategory.SEXUALLY_EXPLICIT),
        result.exists(_.probability == HarmProbability.MEDIUM)
      )
    },

    test("Citation should serialize to JSON correctly") {
      val citation = Citation(
        startIndex = Some(5),
        endIndex = Some(15),
        url = Some("https://test.com"),
        title = Some("Test"),
        license = Some("Apache")
      )
      val expectedJson = "{\"startIndex\":5,\"endIndex\":15,\"url\":\"https://test.com\",\"title\":\"Test\",\"license\":\"Apache\"}"
      val actualJson = citation.toJson
      assertTrue(actualJson == expectedJson)
    },

    test("Citation should deserialize from JSON correctly") {
      val json = """{
        |"startIndex":5,
        |"endIndex":15,
        |"url":"https://test.com",
        |"title":"Test",
        |"license":"Apache"
        |}""".stripMargin
      val result = json.fromJson[Citation]
      assertTrue(
        result.isRight,
        result.exists(_.startIndex == Some(5)),
        result.exists(_.endIndex == Some(15)),
        result.exists(_.url == Some("https://test.com")),
        result.exists(_.title == Some("Test")),
        result.exists(_.license == Some("Apache"))
      )
    },

    test("FinishReason values should serialize to JSON correctly") {
      val reasons = List(
        FinishReason.FINISH_REASON_UNSPECIFIED,
        FinishReason.STOP,
        FinishReason.MAX_TOKENS,
        FinishReason.SAFETY,
        FinishReason.RECITATION,
        FinishReason.OTHER
      )
      val expectedJson = "[\"FINISH_REASON_UNSPECIFIED\",\"STOP\",\"MAX_TOKENS\",\"SAFETY\",\"RECITATION\",\"OTHER\"]"
      val actualJson = reasons.toJson
      assertTrue(actualJson == expectedJson)
    },

    test("HarmProbability values should serialize to JSON correctly") {
      val probabilities = List(
        HarmProbability.HARM_PROBABILITY_UNSPECIFIED,
        HarmProbability.NEGLIGIBLE,
        HarmProbability.LOW,
        HarmProbability.MEDIUM,
        HarmProbability.HIGH
      )
      val expectedJson = "[\"HARM_PROBABILITY_UNSPECIFIED\",\"NEGLIGIBLE\",\"LOW\",\"MEDIUM\",\"HIGH\"]"
      val actualJson = probabilities.toJson
      assertTrue(actualJson == expectedJson)
    },

    test("BlockReason values should serialize to JSON correctly") {
      val reasons = List(
        BlockReason.BLOCK_REASON_UNSPECIFIED,
        BlockReason.SAFETY,
        BlockReason.OTHER
      )
      val expectedJson = "[\"BLOCK_REASON_UNSPECIFIED\",\"SAFETY\",\"OTHER\"]"
      val actualJson = reasons.toJson
      assertTrue(actualJson == expectedJson)
    },

    test("ResponseContent should serialize and deserialize correctly") {
      val content = ResponseContent(
        parts = List(ResponsePart.Text("Test text")),
        role = Some("assistant")
      )
      val json = content.toJson
      val decoded = json.fromJson[ResponseContent]
      assertTrue(
        json == """{"parts":[{"text":"Test text"}],"role":"assistant"}""",
        decoded == Right(content)
      )
    },

    test("ResponsePart.Text should serialize and deserialize correctly") {
      val text = ResponsePart.Text("Generated text")
      val json = text.toJson
      val decoded = json.fromJson[ResponsePart.Text]
      assertTrue(
        json == """{"text":"Generated text"}""",
        decoded == Right(text)
      )
    },

    test("SafetyRating should serialize and deserialize correctly") {
      val rating = SafetyRating(
        category = HarmCategory.HATE_SPEECH,
        probability = HarmProbability.LOW
      )
      val json = rating.toJson
      val decoded = json.fromJson[SafetyRating]
      assertTrue(
        json == """{"category":"HATE_SPEECH","probability":"LOW"}""",
        decoded == Right(rating)
      )
    },

    test("Citation should serialize and deserialize correctly") {
      val citation = Citation(
        startIndex = Some(0),
        endIndex = Some(100),
        url = Some("https://example.com"),
        title = Some("Example"),
        license = Some("CC-BY-4.0")
      )
      val json = citation.toJson
      val decoded = json.fromJson[Citation]
      assertTrue(
        decoded == Right(citation),
        citation.startIndex == Some(0),
        citation.endIndex == Some(100),
        citation.url == Some("https://example.com"),
        citation.title == Some("Example"),
        citation.license == Some("CC-BY-4.0")
      )
    },

    test("CitationMetadata should serialize and deserialize correctly") {
      val metadata = CitationMetadata(
        citations = List(
          Citation(
            startIndex = Some(0),
            endIndex = Some(100),
            url = Some("https://example.com"),
            title = Some("Example"),
            license = Some("CC-BY-4.0")
          )
        )
      )
      val json = metadata.toJson
      val decoded = json.fromJson[CitationMetadata]
      assertTrue(
        decoded == Right(metadata),
        metadata.citations.size == 1
      )
    },

    test("PromptFeedback should serialize and deserialize correctly") {
      val feedback = PromptFeedback(
        blockReason = Some(BlockReason.SAFETY),
        safetyRatings = List(
          SafetyRating(
            category = HarmCategory.HATE_SPEECH,
            probability = HarmProbability.HIGH
          )
        )
      )
      val json = feedback.toJson
      val decoded = json.fromJson[PromptFeedback]
      assertTrue(
        decoded == Right(feedback),
        feedback.blockReason == Some(BlockReason.SAFETY),
        feedback.safetyRatings.size == 1
      )
    },

    test("Candidate should serialize and deserialize correctly") {
      val candidate = Candidate(
        content = ResponseContent(
          parts = List(ResponsePart.Text("Generated text")),
          role = Some("model")
        ),
        finishReason = FinishReason.STOP,
        safetyRatings = List(
          SafetyRating(
            category = HarmCategory.HATE_SPEECH,
            probability = HarmProbability.LOW
          )
        ),
        citationMetadata = Some(
          CitationMetadata(
            citations = List(
              Citation(
                startIndex = Some(0),
                endIndex = Some(100),
                url = Some("https://example.com"),
                title = Some("Example"),
                license = Some("CC-BY-4.0")
              )
            )
          )
        )
      )
      val json = candidate.toJson
      val decoded = json.fromJson[Candidate]
      assertTrue(
        decoded == Right(candidate),
        candidate.content.parts.size == 1,
        candidate.finishReason == FinishReason.STOP,
        candidate.safetyRatings.size == 1,
        candidate.citationMetadata.exists(_.citations.size == 1)
      )
    },

    test("GenerateContentResponse should serialize and deserialize correctly") {
      val response = GenerateContentResponse(
        candidates = List(
          Candidate(
            content = ResponseContent(
              parts = List(ResponsePart.Text("Generated text")),
              role = Some("model")
            ),
            finishReason = FinishReason.STOP,
            safetyRatings = List(
              SafetyRating(
                category = HarmCategory.HATE_SPEECH,
                probability = HarmProbability.LOW
              )
            ),
            citationMetadata = None
          )
        ),
        promptFeedback = Some(
          PromptFeedback(
            blockReason = None,
            safetyRatings = List(
              SafetyRating(
                category = HarmCategory.HATE_SPEECH,
                probability = HarmProbability.LOW
              )
            )
          )
        )
      )
      val json = response.toJson
      val decoded = json.fromJson[GenerateContentResponse]
      assertTrue(
        decoded == Right(response),
        response.candidates.size == 1,
        response.promptFeedback.exists(_.safetyRatings.size == 1)
      )
    },

    test("FinishReason values should be valid") {
      assertTrue(
        FinishReason.values.contains(FinishReason.FINISH_REASON_UNSPECIFIED),
        FinishReason.values.contains(FinishReason.STOP),
        FinishReason.values.contains(FinishReason.MAX_TOKENS),
        FinishReason.values.contains(FinishReason.SAFETY),
        FinishReason.values.contains(FinishReason.RECITATION),
        FinishReason.values.contains(FinishReason.OTHER)
      )
    },

    test("HarmProbability values should be valid") {
      assertTrue(
        HarmProbability.values.contains(HarmProbability.HARM_PROBABILITY_UNSPECIFIED),
        HarmProbability.values.contains(HarmProbability.NEGLIGIBLE),
        HarmProbability.values.contains(HarmProbability.LOW),
        HarmProbability.values.contains(HarmProbability.MEDIUM),
        HarmProbability.values.contains(HarmProbability.HIGH)
      )
    },

    test("BlockReason values should be valid") {
      assertTrue(
        BlockReason.values.contains(BlockReason.BLOCK_REASON_UNSPECIFIED),
        BlockReason.values.contains(BlockReason.SAFETY),
        BlockReason.values.contains(BlockReason.OTHER)
      )
    },

    test("GenerateContentResponse should handle empty candidates") {
      val response = GenerateContentResponse(
        candidates = List.empty,
        promptFeedback = None
      )
      val json = response.toJson
      val decoded = json.fromJson[GenerateContentResponse]
      assertTrue(
        decoded == Right(response),
        response.candidates.isEmpty,
        response.promptFeedback.isEmpty
      )
    },

    test("ResponseContent should handle empty parts") {
      val content = ResponseContent(
        parts = List.empty,
        role = None
      )
      val json = content.toJson
      val decoded = json.fromJson[ResponseContent]
      assertTrue(
        decoded == Right(content),
        content.parts.isEmpty,
        content.role.isEmpty
      )
    },

    test("Candidate should handle minimal configuration") {
      val candidate = Candidate(
        content = ResponseContent(
          parts = List.empty,
          role = None
        ),
        finishReason = FinishReason.FINISH_REASON_UNSPECIFIED,
        safetyRatings = List.empty,
        citationMetadata = None
      )
      val json = candidate.toJson
      val decoded = json.fromJson[Candidate]
      assertTrue(
        decoded == Right(candidate),
        candidate.content.parts.isEmpty,
        candidate.safetyRatings.isEmpty,
        candidate.citationMetadata.isEmpty
      )
    }
  )
} 