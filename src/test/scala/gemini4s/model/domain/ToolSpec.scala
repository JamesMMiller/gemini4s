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
  test("Tool should handle codeExecution") {
    val tool = Tool(codeExecution = Some(CodeExecution()))
    assert(tool.codeExecution.isDefined)
    val json = tool.asJson
    assert(json.asObject.get("codeExecution").isDefined)
  }

  test("CodeExecution should encode/decode") {
    val ce   = CodeExecution()
    val json = ce.asJson.noSpaces
    assertEquals(json, "{}")
    assertEquals(io.circe.parser.decode[CodeExecution](json), Right(ce))
  }

  test("Tool with CodeExecution should encode correctly") {
    val tool = Tool(codeExecution = Some(CodeExecution()))
    val json = tool.asJson.noSpaces
    assert(json.contains("codeExecution"))
  }

  test("Tool decoder should decode correctly") {
    val json    = """{"functionDeclarations":[{"name":"test","description":"desc"}]}"""
    val decoded = io.circe.parser.decode[Tool](json)
    assert(decoded.isRight)
  }

  test("FunctionDeclaration encoder should encode with parameters") {
    val func = FunctionDeclaration("getName", "Get a name", Some(Schema(SchemaType.STRING)))
    val json = func.asJson
    assert(json.hcursor.downField("parameters").succeeded)
  }

  test("Schema encoder should encode complex schema") {
    val schema = Schema(
      `type` = SchemaType.OBJECT,
      properties = Some(Map("name" -> Schema(SchemaType.STRING))),
      required = Some(List("name"))
    )
    val json   = schema.asJson
    assert(json.hcursor.downField("properties").succeeded)
  }

  test("Schema decoder should decode correctly") {
    val json    = """{"type":"STRING","description":"A string field"}"""
    val decoded = io.circe.parser.decode[Schema](json)
    assert(decoded.isRight)
    assertEquals(decoded.map(_.`type`), Right(SchemaType.STRING))
  }

  test("SchemaType encoder should encode all types") {
    assertEquals(SchemaType.STRING.asJson.asString, Some("STRING"))
    assertEquals(SchemaType.NUMBER.asJson.asString, Some("NUMBER"))
    assertEquals(SchemaType.INTEGER.asJson.asString, Some("INTEGER"))
    assertEquals(SchemaType.BOOLEAN.asJson.asString, Some("BOOLEAN"))
    assertEquals(SchemaType.ARRAY.asJson.asString, Some("ARRAY"))
    assertEquals(SchemaType.OBJECT.asJson.asString, Some("OBJECT"))
    assertEquals(SchemaType.TYPE_UNSPECIFIED.asJson.asString, Some("TYPE_UNSPECIFIED"))
  }

  test("SchemaType decoder should decode all types") {
    assertEquals(io.circe.parser.decode[SchemaType](""""STRING""""), Right(SchemaType.STRING))
    assertEquals(io.circe.parser.decode[SchemaType](""""NUMBER""""), Right(SchemaType.NUMBER))
    assertEquals(io.circe.parser.decode[SchemaType](""""INTEGER""""), Right(SchemaType.INTEGER))
    assertEquals(io.circe.parser.decode[SchemaType](""""BOOLEAN""""), Right(SchemaType.BOOLEAN))
    assertEquals(io.circe.parser.decode[SchemaType](""""ARRAY""""), Right(SchemaType.ARRAY))
    assertEquals(io.circe.parser.decode[SchemaType](""""OBJECT""""), Right(SchemaType.OBJECT))
  }

  test("FunctionCallingMode encoder should encode all modes") {
    assertEquals(FunctionCallingMode.AUTO.asJson.asString, Some("AUTO"))
    assertEquals(FunctionCallingMode.ANY.asJson.asString, Some("ANY"))
    assertEquals(FunctionCallingMode.NONE.asJson.asString, Some("NONE"))
    assertEquals(FunctionCallingMode.MODE_UNSPECIFIED.asJson.asString, Some("MODE_UNSPECIFIED"))
  }

  test("FunctionCallingMode decoder should decode all modes") {
    assertEquals(io.circe.parser.decode[FunctionCallingMode](""""AUTO""""), Right(FunctionCallingMode.AUTO))
    assertEquals(io.circe.parser.decode[FunctionCallingMode](""""ANY""""), Right(FunctionCallingMode.ANY))
    assertEquals(io.circe.parser.decode[FunctionCallingMode](""""NONE""""), Right(FunctionCallingMode.NONE))
  }

  test("FunctionCallingConfig decoder should decode correctly") {
    val json    = """{"mode":"AUTO","allowedFunctionNames":["func1"]}"""
    val decoded = io.circe.parser.decode[FunctionCallingConfig](json)
    assert(decoded.isRight)
  }

  test("ToolConfig encoder and decoder should work") {
    val config  = ToolConfig(Some(FunctionCallingConfig(Some(FunctionCallingMode.ANY))))
    val json    = config.asJson.noSpaces
    val decoded = io.circe.parser.decode[ToolConfig](json)
    assertEquals(decoded, Right(config))
  }
}
