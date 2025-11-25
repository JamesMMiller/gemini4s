package gemini4s.model

import munit.FunSuite

import gemini4s.model.domain._
import gemini4s.model.request._

class GeminiDefaultsSpec extends FunSuite {

  test("GenerateContentRequest defaults") {
    val req = GenerateContentRequest(ModelName.unsafe("model"), List(Content(List(ContentPart("text")))))
    assertEquals(req.safetySettings, None)
    assertEquals(req.generationConfig, None)
    assertEquals(req.systemInstruction, None)
    assertEquals(req.tools, None)
    assertEquals(req.toolConfig, None)
  }

  test("EmbedContentRequest defaults") {
    val req = EmbedContentRequest(Content(List(ContentPart("text"))), ModelName.unsafe("model"))
    assertEquals(req.taskType, None)
    assertEquals(req.title, None)
    assertEquals(req.outputDimensionality, None)
  }

  test("CreateCachedContentRequest defaults") {
    val req = CreateCachedContentRequest()
    assertEquals(req.model, None)
    assertEquals(req.systemInstruction, None)
    assertEquals(req.contents, None)
    assertEquals(req.tools, None)
    assertEquals(req.toolConfig, None)
    assertEquals(req.ttl, None)
    assertEquals(req.displayName, None)
  }

  test("Content defaults") {
    val content = Content(List(ContentPart("text")))
    assertEquals(content.role, None)
  }

  test("GenerationConfig defaults") {
    val config = GenerationConfig()
    assertEquals(config.temperature, None)
    assertEquals(config.topK, None)
    assertEquals(config.topP, None)
    assertEquals(config.candidateCount, None)
    assertEquals(config.maxOutputTokens, None)
    assertEquals(config.stopSequences, None)
    assertEquals(config.responseMimeType, None)
  }

  test("Tool defaults") {
    val tool = Tool()
    assertEquals(tool.functionDeclarations, None)
  }

  test("ToolConfig defaults") {
    val config = ToolConfig()
    assertEquals(config.functionCallingConfig, None)
  }

  test("FunctionCallingConfig defaults") {
    val config = FunctionCallingConfig()
    assertEquals(config.mode, None)
    assertEquals(config.allowedFunctionNames, None)
  }

  test("FunctionDeclaration defaults") {
    val func = FunctionDeclaration("name", "desc")
    assertEquals(func.parameters, None)
  }

  test("Schema defaults") {
    val schema = Schema(SchemaType.STRING)
    assertEquals(schema.format, None)
    assertEquals(schema.description, None)
    assertEquals(schema.nullable, None)
    assertEquals(schema.`enum`, None)
    assertEquals(schema.properties, None)
    assertEquals(schema.required, None)
  }
}
