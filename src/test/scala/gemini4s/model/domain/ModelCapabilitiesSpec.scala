package gemini4s.model.domain

import munit.FunSuite
import io.circe.syntax._
import io.circe.parser._
import ModelCapabilities._

class ModelCapabilitiesSpec extends FunSuite {

  test("Pre-defined models should have correct names") {
    assertEquals(Model.gemini25Flash.value, "gemini-2.5-flash")
    assertEquals(Model.gemini25Pro.value, "gemini-2.5-pro")
    assertEquals(Model.embeddingGemini001.value, "gemini-embedding-001")
  }

  test("Model.toModelName should return the underlying ModelName") {
    val model = Model.gemini25Flash
    assertEquals(model.toModelName, ModelName.Gemini25Flash)
  }

  test("Model encoder should encode as model name") {
    val encoded = Model.gemini25Flash.asJson
    assertEquals(encoded.asString, Some("models/gemini-2.5-flash"))
  }

  test("Model decoder should decode model name") {
    val result = decode[Model[CanGenerate]]("\"models/gemini-2.5-flash\"")
    assert(result.isRight)
    assertEquals(result.toOption.get.value, "gemini-2.5-flash")
  }

  test("generationModel should create model with full capabilities") {
    val model = Model.generationModel("custom-model")
    assertEquals(model.value, "custom-model")
  }

  test("embeddingModel should create model with embedding capabilities") {
    val model = Model.embeddingModel("custom-embed")
    assertEquals(model.value, "custom-embed")
  }

  test("basicModel should create model with minimal capabilities") {
    val model = Model.basicModel("minimal")
    assertEquals(model.value, "minimal")
  }

  // Type-level tests (these verify compile-time constraints)
  test("SupportsGeneration evidence should exist for generation models") {
    val evidence  = summon[SupportsGeneration[Model[CanGenerate]]]
    val modelName = evidence.toModelName(Model.basicModel("test"))
    assertEquals(modelName.value, "test")
  }

  test("SupportsGeneration evidence should exist for ModelName") {
    val evidence  = summon[SupportsGeneration[ModelName]]
    val modelName = evidence.toModelName(ModelName.Gemini25Flash)
    assertEquals(modelName.value, "gemini-2.5-flash")
  }

  test("SupportsEmbedding evidence should exist for embedding models") {
    val evidence  = summon[SupportsEmbedding[Model[EmbeddingCapabilities]]]
    val modelName = evidence.toModelName(Model.embeddingModel("embed-test"))
    assertEquals(modelName.value, "embed-test")
  }

  test("SupportsTokenCount evidence should exist for countable models") {
    val evidence = summon[SupportsTokenCount[Model[CanCount]]]
    assert(evidence != null)
  }

  test("SupportsCaching evidence should exist for cacheable models") {
    val evidence = summon[SupportsCaching[Model[CanCache]]]
    assert(evidence != null)
  }

  test("SupportsBatch evidence should exist for batchable models") {
    val evidence = summon[SupportsBatch[Model[CanBatch]]]
    assert(evidence != null)
  }

  // Type-level safety demonstration tests
  // Note: These tests verify compile-time constraints by proving evidence exists

  test("FullGenerationCapabilities models should have all generation evidence") {
    // This tests that Model[FullGenerationCapabilities] satisfies all generation type classes
    summon[SupportsGeneration[Model[FullGenerationCapabilities]]]
    summon[SupportsTokenCount[Model[FullGenerationCapabilities]]]
    summon[SupportsCaching[Model[FullGenerationCapabilities]]]
    summon[SupportsBatch[Model[FullGenerationCapabilities]]]
  }

  test("EmbeddingCapabilities models should have embedding and count evidence") {
    summon[SupportsEmbedding[Model[EmbeddingCapabilities]]]
    summon[SupportsTokenCount[Model[EmbeddingCapabilities]]]
  }
}
