package gemini4s.model

import zio.json._
import zio.test.Assertion._
import zio.test._

import gemini4s.model.GeminiCodecs.given
import gemini4s.model.GeminiRequest._

object GeminiRequestSpec extends ZIOSpecDefault {
  def spec = suite("GeminiRequest")(
    // test("Content should serialize to JSON correctly") {
    //   val expected = Content.Text("Test prompt")
    //   val json = expected.toJson
    //   val parsed = json.fromJson[Content]

    //   assertTrue(
    //     json == """{"parts":[{"text":"Test prompt"}]}""",
    //     parsed == Right(expected)
    //   )
    // }, 

    test("SafetySetting should serialize to JSON correctly") {
      val expected = SafetySetting(
        category = HarmCategory.HARASSMENT,
        threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
      )
      val json = expected.toJson
      val parsed = json.fromJson[SafetySetting]

      assertTrue(
        json == """{"category":"HARASSMENT","threshold":"BLOCK_MEDIUM_AND_ABOVE"}""",
        parsed == Right(expected)
      )
    },

    test("GenerationConfig should serialize to JSON correctly") {
      val expected = GenerationConfig(
        temperature = Some(0.8f),
        candidateCount = Some(2),
        maxOutputTokens = Some(100),
        topP = Some(0.9f),
        topK = Some(10),
        stopSequences = Some(List(".", "!"))
      )
      val json = expected.toJson
      val parsed = json.fromJson[GenerationConfig]

      assertTrue(
        json == """{"temperature":0.8,"topK":10,"topP":0.9,"candidateCount":2,"maxOutputTokens":100,"stopSequences":[".","!"]}""",
        parsed == Right(expected)
      )
    },

    // test("GenerateContent should serialize to JSON correctly") {
    //   val expected = GenerateContent(
    //     contents = List(Content.Text("Test prompt")),
    //     safetySettings = Some(List(
    //       SafetySetting(
    //         category = HarmCategory.HARASSMENT,
    //         threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
    //       )
    //     )),
    //     generationConfig = Some(
    //       GenerationConfig(
    //         temperature = Some(0.8f),
    //         candidateCount = Some(2),
    //         maxOutputTokens = Some(100),
    //         topP = Some(0.9f),
    //         topK = Some(10),
    //         stopSequences = Some(List(".", "!"))
    //       )
    //     )
    //   )
    //   val json = expected.toJson
    //   val parsed = json.fromJson[GenerateContent]

    //   assertTrue(
    //     json == """{"contents":[{"parts":[{"text":"Test prompt"}]}],"safetySettings":[{"category":"HARASSMENT","threshold":"BLOCK_MEDIUM_AND_ABOVE"}],"generationConfig":{"temperature":0.8,"topK":10,"topP":0.9,"candidateCount":2,"maxOutputTokens":100,"stopSequences":[".","!"]}}""",
    //     parsed == Right(expected)
    //   )
    // }
  )
} 