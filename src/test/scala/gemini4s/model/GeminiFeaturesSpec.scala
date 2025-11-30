package gemini4s.model

import io.circe.parser._
import io.circe.syntax._
import munit.FunSuite

import gemini4s.model.domain._
import gemini4s.model.request._
import gemini4s.model.response._

class GeminiServiceFeaturesSpec extends FunSuite {

  test("EmbedContentRequest should encode correctly") {
    val request = EmbedContentRequest(
      content = Content(parts = List(ContentPart.Text("Hello"))),
      model = ModelName.unsafe("models/embedding-001"),
      taskType = Some(TaskType.RETRIEVAL_DOCUMENT),
      title = Some("Test Doc"),
      outputDimensionality = Some(768)
    )
    val json    = request.asJson.noSpaces
    assert(json.contains("RETRIEVAL_DOCUMENT"))
    assert(json.contains("Test Doc"))
    assert(json.contains("768"))
  }

  test("BatchEmbedContentsRequest should encode correctly") {
    val request = BatchEmbedContentsRequest(
      model = ModelName.unsafe("models/embedding-001"),
      requests = List(
        EmbedContentRequest(
          content = Content(parts = List(ContentPart.Text("Hello"))),
          model = ModelName.unsafe("models/embedding-001")
        ),
        EmbedContentRequest(
          content = Content(parts = List(ContentPart.Text("World"))),
          model = ModelName.unsafe("models/embedding-001")
        )
      )
    )
    val json    = request.asJson
    val cursor  = json.hcursor

    assert(cursor.downField("requests").succeeded)
    assert(cursor.downField("requests").values.exists(_.size == 2))

    val firstReq = cursor.downField("requests").downArray
    assert(firstReq.downField("model").as[String].contains("models/embedding-001"))
    assert(firstReq.downField("content").downField("parts").downArray.downField("text").as[String].contains("Hello"))
  }

  test("EmbedContentResponse should decode correctly") {
    val json   = """{"embedding": {"values": [0.1, 0.2, 0.3]}}"""
    val result = decode[EmbedContentResponse](json)
    assert(result.isRight)
    assertEquals(result.toOption.get.embedding.values, List(0.1f, 0.2f, 0.3f))
  }

  test("BatchEmbedContentsResponse should decode correctly") {
    val json       = """{"embeddings": [{"values": [0.1, 0.2]}, {"values": [0.3, 0.4]}]}"""
    val result     = decode[BatchEmbedContentsResponse](json)
    assert(result.isRight)
    val embeddings = result.toOption.get.embeddings
    assertEquals(embeddings.length, 2)
    assertEquals(embeddings.head.values, List(0.1f, 0.2f))
    assertEquals(embeddings(1).values, List(0.3f, 0.4f))
  }

  test("CreateCachedContentRequest should encode correctly") {
    val request = CreateCachedContentRequest(
      model = Some("models/gemini-pro"),
      contents = Some(List(Content(parts = List(ContentPart.Text("Cached context"))))),
      ttl = Some("3600s")
    )
    val json    = request.asJson.noSpaces
    assert(json.contains("gemini-pro"))
    assert(json.contains("Cached context"))
    assert(json.contains("3600s"))
  }

  test("CachedContent response should decode correctly") {
    val json = """
        |{
        |  "name": "cachedContents/123",
        |  "model": "models/gemini-pro",
        |  "createTime": "2024-01-01T00:00:00Z",
        |  "updateTime": "2024-01-01T00:00:00Z",
        |  "expireTime": "2024-01-01T01:00:00Z",
        |  "displayName": "My Cache"
        |}
        |""".stripMargin

    val result  = decode[CachedContent](json)
    assert(result.isRight)
    val content = result.toOption.get
    assertEquals(content.name, "cachedContents/123")
    assertEquals(content.displayName, Some("My Cache"))
  }
}
