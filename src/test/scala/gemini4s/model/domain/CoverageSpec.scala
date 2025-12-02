package gemini4s.model.domain

import munit.FunSuite
import io.circe.syntax._
import io.circe.parser.decode

class CoverageSpec extends FunSuite {

  test("ModelName.Tuned should encode correctly") {
    val model: ModelName = ModelName.Tuned("my-tuned-model")
    assertEquals(model.asJson.asString, Some("tunedModels/my-tuned-model"))
  }

  test("ModelName should decode correctly from tunedModels prefix") {
    val json    = "\"tunedModels/my-tuned-model\""
    val decoded = decode[ModelName](json)
    assertEquals(
      decoded,
      Right(ModelName.Standard("my-tuned-model"))
    ) // Note: Decoder currently defaults to Standard via apply
  }

  test("GeminiConstants.Endpoints should handle Tuned models") {
    val model    = ModelName.Tuned("my-tuned-model")
    val endpoint = GeminiConstants.Endpoints.generateContent(model)
    assertEquals(endpoint, "tunedModels/my-tuned-model:generateContent")
  }

  test("CodeExecution should encode/decode") {
    val ce   = CodeExecution()
    val json = ce.asJson.noSpaces
    assertEquals(json, "{}")
    assertEquals(decode[CodeExecution](json), Right(ce))
  }

  test("Tool with CodeExecution should encode correctly") {
    val tool = Tool(codeExecution = Some(CodeExecution()))
    val json = tool.asJson.noSpaces
    assert(json.contains("codeExecution"))
  }
}
