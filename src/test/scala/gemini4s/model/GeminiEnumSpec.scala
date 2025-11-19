package gemini4s.model

import io.circe._
import io.circe.syntax._
import munit.FunSuite

class GeminiEnumSpec extends FunSuite {

  import GeminiRequest._

  test("TaskType should decode valid values") {
    TaskType.values.foreach { value =>
      assertEquals(Json.fromString(value.toString).as[TaskType], Right(value))
    }
  }

  test("TaskType should fail on invalid values") {
    assert(Json.fromString("INVALID").as[TaskType].isLeft)
  }

  test("HarmCategory should decode valid values") {
    HarmCategory.values.foreach { value =>
      assertEquals(Json.fromString(value.toString).as[HarmCategory], Right(value))
    }
  }

  test("HarmCategory should fail on invalid values") {
    assert(Json.fromString("INVALID").as[HarmCategory].isLeft)
  }

  test("HarmBlockThreshold should decode valid values") {
    HarmBlockThreshold.values.foreach { value =>
      assertEquals(Json.fromString(value.toString).as[HarmBlockThreshold], Right(value))
    }
  }

  test("HarmBlockThreshold should fail on invalid values") {
    assert(Json.fromString("INVALID").as[HarmBlockThreshold].isLeft)
  }

  test("FunctionCallingMode should decode valid values") {
    FunctionCallingMode.values.foreach { value =>
      assertEquals(Json.fromString(value.toString).as[FunctionCallingMode], Right(value))
    }
  }

  test("FunctionCallingMode should fail on invalid values") {
    assert(Json.fromString("INVALID").as[FunctionCallingMode].isLeft)
  }

  test("SchemaType should decode valid values") {
    SchemaType.values.foreach { value =>
      assertEquals(Json.fromString(value.toString).as[SchemaType], Right(value))
    }
  }

  test("SchemaType should fail on invalid values") {
    assert(Json.fromString("INVALID").as[SchemaType].isLeft)
  }
}

