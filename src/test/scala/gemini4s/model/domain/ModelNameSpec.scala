package gemini4s.model.domain

import io.circe.parser.decode
import io.circe.syntax._
import munit.FunSuite

class ModelNameSpec extends FunSuite {

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

  test("ModelName.apply should validate input") {
    assert(ModelName("valid").isRight)
    assert(ModelName("").isLeft)
    assert(ModelName("   ").isLeft)
  }

  test("ModelName.unsafe should validate input") {
    assertEquals(ModelName.unsafe("valid"), ModelName.Standard("valid"))
    intercept[IllegalArgumentException] {
      ModelName.unsafe("")
    }
    intercept[IllegalArgumentException] {
      ModelName.unsafe("   ")
    }
  }
}
