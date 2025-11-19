package gemini4s.model

import io.circe.parser._
import io.circe.syntax._
import munit.FunSuite

import gemini4s.model.GeminiRequest._

class GeminiRequestSpec extends FunSuite {

  test("GenerateContent encoder should include all fields") {
    val request = GenerateContent(
      contents = List(Content(parts = List(Part(text = "Hello")))),
      safetySettings = Some(List(SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.BLOCK_NONE))),
      generationConfig = Some(GenerationConfig(temperature = Some(0.5f), responseMimeType = Some("application/json"))),
      systemInstruction = Some(Content(parts = List(Part(text = "Be helpful")))),
      tools = Some(List(Tool(functionDeclarations = Some(List(FunctionDeclaration("func", "desc")))))),
      toolConfig = Some(ToolConfig(functionCallingConfig = Some(FunctionCallingConfig(mode = Some(FunctionCallingMode.AUTO)))))
    )

    val json = request.asJson.noSpaces
    assert(json.contains("responseMimeType"))
    assert(json.contains("tools"))
    assert(json.contains("toolConfig"))
    assert(json.contains("systemInstruction"))
  }

  test("GenerationConfig should encode responseMimeType") {
    val config = GenerationConfig(responseMimeType = Some("application/json"))
    val json = config.asJson.noSpaces
    assert(json.contains(""""responseMimeType":"application/json""""))
  }

  test("ToolConfig should encode functionCallingConfig") {
    val config = ToolConfig(functionCallingConfig = Some(FunctionCallingConfig(mode = Some(FunctionCallingMode.AUTO))))
    val json = config.asJson.noSpaces
    assert(json.contains(""""mode":"AUTO""""))
  }

  test("Schema encoder/decoder") {
    val schema = Schema(
      `type` = SchemaType.OBJECT,
      properties = Some(Map("field" -> Schema(`type` = SchemaType.STRING)))
    )
    val json = schema.asJson
    val decoded = json.as[Schema]
    assertEquals(decoded, Right(schema))
  }

}
