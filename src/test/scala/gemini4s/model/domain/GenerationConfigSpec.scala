package gemini4s.model.domain

import io.circe.parser._
import io.circe.syntax._
import munit.FunSuite

class GenerationConfigSpec extends FunSuite {

  test("GenerationConfig should encode with all fields") {
    val config = GenerationConfig(
      temperature = Some(Temperature.unsafe(0.7f)),
      topK = Some(TopK.unsafe(40)),
      topP = Some(TopP.unsafe(0.95f)),
      candidateCount = Some(1),
      maxOutputTokens = Some(2048),
      stopSequences = Some(List("STOP")),
      responseMimeType = Some(MimeType.TextPlain)
    )

    val json = config.asJson
    assert(json.asObject.isDefined)
  }

  test("GenerationConfig should encode with minimal fields") {
    val config = GenerationConfig(temperature = Some(Temperature.unsafe(0.5f)))
    val json   = config.asJson
    assert(json.asObject.isDefined)
  }

  test("GenerationConfig should decode from JSON") {
    val jsonString = """{"temperature":0.7,"topK":40,"topP":0.95}"""
    val result     = decode[GenerationConfig](jsonString)
    assert(result.isRight)
    assertEquals(result.map(_.temperature.map(_.value)), Right(Some(0.7f)))
  }

  test("GenerationConfig should handle empty config") {
    val config = GenerationConfig()
    assertEquals(config.temperature, None)
    assertEquals(config.topK, None)
    assertEquals(config.topP, None)
  }

  test("GenerationConfig should handle responseMimeType") {
    val config = GenerationConfig(responseMimeType = Some(MimeType.ApplicationJson))
    assertEquals(config.responseMimeType.map(_.value), Some("application/json"))
  }

  test("GenerationConfig should handle stopSequences") {
    val config = GenerationConfig(stopSequences = Some(List("STOP", "END")))
    assertEquals(config.stopSequences, Some(List("STOP", "END")))
  }
  test("GenerationConfig should handle responseSchema") {
    val schema = Schema(SchemaType.OBJECT, properties = Some(Map("key" -> Schema(SchemaType.STRING))))
    val config = GenerationConfig(responseSchema = Some(schema))
    assertEquals(config.responseSchema, Some(schema))
    val json   = config.asJson
    assert(json.asObject.get("responseSchema").isDefined)
  }
}
