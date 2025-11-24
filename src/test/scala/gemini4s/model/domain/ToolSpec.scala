package gemini4s.model.domain

import io.circe.syntax._
import munit.FunSuite

class ToolSpec extends FunSuite {

  test("Tool should create with function declarations") {
    val func = FunctionDeclaration("test", "description", None)
    val tool = Tool(functionDeclarations = Some(List(func)))
    assert(tool.functionDeclarations.isDefined)
    assertEquals(tool.functionDeclarations.get.length, 1)
  }

  test("Tool should create without function declarations") {
    val tool = Tool()
    assertEquals(tool.functionDeclarations, None)
  }

  test("Tool should encode to JSON") {
    val tool = Tool()
    val json = tool.asJson
    assert(json.asObject.isDefined)
  }

  test("FunctionDeclaration should create with parameters") {
    val schema = Schema(SchemaType.STRING)
    val func   = FunctionDeclaration("getName", "Gets a name", Some(schema))
    assertEquals(func.name, "getName")
    assertEquals(func.description, "Gets a name")
    assert(func.parameters.isDefined)
  }

  test("FunctionDeclaration should create without parameters") {
    val func = FunctionDeclaration("greet", "Greets someone")
    assertEquals(func.parameters, None)
  }

  test("ToolConfig should create with function calling config") {
    val config     = FunctionCallingConfig(mode = Some(FunctionCallingMode.AUTO))
    val toolConfig = ToolConfig(functionCallingConfig = Some(config))
    assert(toolConfig.functionCallingConfig.isDefined)
  }

  test("ToolConfig should create without function calling config") {
    val toolConfig = ToolConfig()
    assertEquals(toolConfig.functionCallingConfig, None)
  }

  test("FunctionCallingConfig should handle mode") {
    val config = FunctionCallingConfig(mode = Some(FunctionCallingMode.NONE))
    assertEquals(config.mode, Some(FunctionCallingMode.NONE))
  }

  test("FunctionCallingConfig should handle allowed function names") {
    val config = FunctionCallingConfig(
      mode = Some(FunctionCallingMode.ANY),
      allowedFunctionNames = Some(List("func1", "func2"))
    )
    assertEquals(config.allowedFunctionNames, Some(List("func1", "func2")))
  }
}
