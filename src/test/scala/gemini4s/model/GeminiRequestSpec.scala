package gemini4s.model

import zio.json._
import zio.test.Assertion._
import zio.test._

import GeminiRequest._
import GeminiCodecs.given

object GeminiRequestSpec extends ZIOSpecDefault {
  def spec = suite("GeminiRequest")(
    test("GenerateContent should serialize to JSON correctly") {
      val request = GenerateContent(
        contents = List(Content.Text("Test prompt")),
        safetySettings = Some(List(
          SafetySetting(
            category = HarmCategory.HARASSMENT,
            threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
          )
        )),
        generationConfig = Some(
          GenerationConfig(
            temperature = Some(0.7f),
            topK = Some(40),
            topP = Some(0.95f),
            candidateCount = Some(1),
            maxOutputTokens = Some(1024),
            stopSequences = Some(List(".", "?", "!"))
          )
        )
      )

      val expectedJson = """{
        |  "contents" : [
        |    {
        |      "text" : "Test prompt"
        |    }
        |  ],
        |  "safetySettings" : [
        |    {
        |      "category" : "HARASSMENT",
        |      "threshold" : "BLOCK_MEDIUM_AND_ABOVE"
        |    }
        |  ],
        |  "generationConfig" : {
        |    "temperature" : 0.7,
        |    "topK" : 40,
        |    "topP" : 0.95,
        |    "candidateCount" : 1,
        |    "maxOutputTokens" : 1024,
        |    "stopSequences" : [
        |      ".",
        |      "?",
        |      "!"
        |    ]
        |  }
        |}""".stripMargin

      val actualJson = request.toJson
      assertTrue(actualJson == expectedJson)
    },

    test("GenerateContent should deserialize from JSON correctly") {
      val json = """{
        |  "contents" : [
        |    {
        |      "text" : "Test prompt"
        |    }
        |  ],
        |  "safetySettings" : [
        |    {
        |      "category" : "HARASSMENT",
        |      "threshold" : "BLOCK_MEDIUM_AND_ABOVE"
        |    }
        |  ],
        |  "generationConfig" : {
        |    "temperature" : 0.7,
        |    "topK" : 40,
        |    "topP" : 0.95,
        |    "candidateCount" : 1,
        |    "maxOutputTokens" : 1024,
        |    "stopSequences" : [
        |      ".",
        |      "?",
        |      "!"
        |    ]
        |  }
        |}""".stripMargin

      val expected = GenerateContent(
        contents = List(Content.Text("Test prompt")),
        safetySettings = Some(List(
          SafetySetting(
            category = HarmCategory.HARASSMENT,
            threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
          )
        )),
        generationConfig = Some(
          GenerationConfig(
            temperature = Some(0.7f),
            topK = Some(40),
            topP = Some(0.95f),
            candidateCount = Some(1),
            maxOutputTokens = Some(1024),
            stopSequences = Some(List(".", "?", "!"))
          )
        )
      )

      val result = json.fromJson[GenerateContent]
      assertTrue(
        result.isRight,
        result.exists(_.contents == expected.contents),
        result.exists(_.safetySettings == expected.safetySettings),
        result.exists(_.generationConfig == expected.generationConfig)
      )
    },

    test("Content.Text should serialize to JSON correctly") {
      val content = Content.Text("Test text")
      val expectedJson = """{"text":"Test text"}"""
      val actualJson = content.toJson
      assertTrue(actualJson == expectedJson)
    },

    test("Content.Text should deserialize from JSON correctly") {
      val json = """{"text":"Test text"}"""
      val result = json.fromJson[Content]
      assertTrue(
        result.isRight,
        result.exists(_ == Content.Text("Test text"))
      )
    },

    test("SafetySetting should serialize to JSON correctly") {
      val setting = SafetySetting(
        category = HarmCategory.HATE_SPEECH,
        threshold = HarmBlockThreshold.BLOCK_LOW_AND_ABOVE
      )
      val expectedJson = """{"category":"HATE_SPEECH","threshold":"BLOCK_LOW_AND_ABOVE"}"""
      val actualJson = setting.toJson
      assertTrue(actualJson == expectedJson)
    },

    test("SafetySetting should deserialize from JSON correctly") {
      val json = """{"category":"HATE_SPEECH","threshold":"BLOCK_LOW_AND_ABOVE"}"""
      val result = json.fromJson[SafetySetting]
      assertTrue(
        result.isRight,
        result.exists(_.category == HarmCategory.HATE_SPEECH),
        result.exists(_.threshold == HarmBlockThreshold.BLOCK_LOW_AND_ABOVE)
      )
    },

    test("GenerationConfig should serialize to JSON correctly") {
      val config = GenerationConfig(
        temperature = Some(0.8f),
        topK = Some(50),
        topP = Some(0.9f),
        candidateCount = Some(2),
        maxOutputTokens = Some(2048),
        stopSequences = Some(List("END"))
      )
      val expectedJson = "{\"temperature\":0.8,\"topK\":50,\"topP\":0.9,\"candidateCount\":2,\"maxOutputTokens\":2048,\"stopSequences\":[\"END\"]}"
      val actualJson = config.toJson
      assertTrue(actualJson == expectedJson)
    },

    test("GenerationConfig should deserialize from JSON correctly") {
      val json = "{\"temperature\":0.8,\"topK\":50,\"topP\":0.9,\"candidateCount\":2,\"maxOutputTokens\":2048,\"stopSequences\":[\"END\"]}"
      val result = json.fromJson[GenerationConfig]
      assertTrue(
        result.isRight,
        result.exists(_.temperature == Some(0.8f)),
        result.exists(_.topK == Some(50)),
        result.exists(_.topP == Some(0.9f)),
        result.exists(_.candidateCount == Some(2)),
        result.exists(_.maxOutputTokens == Some(2048)),
        result.exists(_.stopSequences == Some(List("END")))
      )
    },

    test("HarmCategory values should serialize to JSON correctly") {
      val categories = List(
        HarmCategory.HARM_CATEGORY_UNSPECIFIED,
        HarmCategory.HARASSMENT,
        HarmCategory.HATE_SPEECH,
        HarmCategory.SEXUALLY_EXPLICIT,
        HarmCategory.DANGEROUS_CONTENT
      )
      val expectedJson = "[\"HARM_CATEGORY_UNSPECIFIED\",\"HARASSMENT\",\"HATE_SPEECH\",\"SEXUALLY_EXPLICIT\",\"DANGEROUS_CONTENT\"]"
      val actualJson = categories.toJson
      assertTrue(actualJson == expectedJson)
    },

    test("HarmBlockThreshold values should serialize to JSON correctly") {
      val thresholds = List(
        HarmBlockThreshold.HARM_BLOCK_THRESHOLD_UNSPECIFIED,
        HarmBlockThreshold.BLOCK_LOW_AND_ABOVE,
        HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
        HarmBlockThreshold.BLOCK_ONLY_HIGH,
        HarmBlockThreshold.BLOCK_NONE
      )
      val expectedJson = "[\"HARM_BLOCK_THRESHOLD_UNSPECIFIED\",\"BLOCK_LOW_AND_ABOVE\",\"BLOCK_MEDIUM_AND_ABOVE\",\"BLOCK_ONLY_HIGH\",\"BLOCK_NONE\"]"
      val actualJson = thresholds.toJson
      assertTrue(actualJson == expectedJson)
    },

    test("Content.Text should serialize and deserialize correctly") {
      val text = Content.Text("Hello, world!")
      val json = text.toJson
      val decoded = json.fromJson[Content.Text]
      assertTrue(
        json == """{"text":"Hello, world!"}""",
        decoded == Right(text)
      )
    },

    test("SafetySetting should serialize and deserialize correctly") {
      val setting = SafetySetting(
        category = HarmCategory.HATE_SPEECH,
        threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
      )
      val json = setting.toJson
      val decoded = json.fromJson[SafetySetting]
      assertTrue(
        json == """{"category":"HATE_SPEECH","threshold":"BLOCK_MEDIUM_AND_ABOVE"}""",
        decoded == Right(setting)
      )
    },

    test("GenerationConfig should serialize and deserialize correctly") {
      val config = GenerationConfig(
        temperature = Some(0.7f),
        topK = Some(40),
        topP = Some(0.95f),
        candidateCount = Some(1),
        maxOutputTokens = Some(1024),
        stopSequences = Some(List(".", "?", "!"))
      )
      val json = config.toJson
      val decoded = json.fromJson[GenerationConfig]
      assertTrue(
        decoded == Right(config),
        config.temperature == Some(0.7f),
        config.topK == Some(40),
        config.topP == Some(0.95f),
        config.candidateCount == Some(1),
        config.maxOutputTokens == Some(1024),
        config.stopSequences == Some(List(".", "?", "!"))
      )
    },

    test("GenerateContent should serialize and deserialize correctly") {
      val request = GenerateContent(
        contents = List(Content.Text("Test prompt")),
        safetySettings = Some(List(
          SafetySetting(
            category = HarmCategory.HARASSMENT,
            threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
          )
        )),
        generationConfig = Some(
          GenerationConfig(
            temperature = Some(0.7f),
            topK = Some(40),
            topP = Some(0.95f),
            candidateCount = Some(1),
            maxOutputTokens = Some(1024),
            stopSequences = Some(List(".", "?", "!"))
          )
        )
      )
      val json = request.toJson
      val decoded = json.fromJson[GenerateContent]
      assertTrue(
        decoded == Right(request),
        request.contents.size == 1,
        request.safetySettings.exists(_.size == 1),
        request.generationConfig.exists(_.temperature == Some(0.7f))
      )
    },

    test("HarmCategory values should be valid") {
      assertTrue(
        HarmCategory.values.contains(HarmCategory.HARM_CATEGORY_UNSPECIFIED),
        HarmCategory.values.contains(HarmCategory.HARASSMENT),
        HarmCategory.values.contains(HarmCategory.HATE_SPEECH),
        HarmCategory.values.contains(HarmCategory.SEXUALLY_EXPLICIT),
        HarmCategory.values.contains(HarmCategory.DANGEROUS_CONTENT)
      )
    },

    test("HarmBlockThreshold values should be valid") {
      assertTrue(
        HarmBlockThreshold.values.contains(HarmBlockThreshold.HARM_BLOCK_THRESHOLD_UNSPECIFIED),
        HarmBlockThreshold.values.contains(HarmBlockThreshold.BLOCK_LOW_AND_ABOVE),
        HarmBlockThreshold.values.contains(HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE),
        HarmBlockThreshold.values.contains(HarmBlockThreshold.BLOCK_ONLY_HIGH),
        HarmBlockThreshold.values.contains(HarmBlockThreshold.BLOCK_NONE)
      )
    }
  )
} 