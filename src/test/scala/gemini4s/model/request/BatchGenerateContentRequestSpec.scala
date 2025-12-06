package gemini4s.model.request

import io.circe.parser.*
import io.circe.syntax.*
import munit.FunSuite

import gemini4s.model.domain.*

class BatchGenerateContentRequestSpec extends FunSuite {

  test("BatchGenerateContentRequest encoder should handle inline requests") {
    val model    = GeminiConstants.DefaultModel
    val requests = List(
      GenerateContentRequest(
        model,
        List(Content(List(ContentPart.Text("Hello"))))
      )
    )
    val req      = BatchGenerateContentRequest(requests)
    val json     = req.asJson

    assert(json.hcursor.downField("batch").downField("input_config").downField("requests").succeeded)
  }

  test("BatchGenerateContentRequest encoder should handle GCS URI") {
    val req  = BatchGenerateContentRequest("gs://bucket/path/file.jsonl")
    val json = req.asJson

    assert(json.hcursor.downField("batch").downField("input_config").downField("gcs_source").downField("uri").succeeded)
    assertEquals(
      json.hcursor.downField("batch").downField("input_config").downField("gcs_source").downField("uri").as[String],
      Right("gs://bucket/path/file.jsonl")
    )
  }

  test("BatchGenerateContentRequest encoder should handle file URI") {
    val fileUri = "https://generativelanguage.googleapis.com/v1beta/files/abc123"
    val req     = BatchGenerateContentRequest(fileUri)
    val json    = req.asJson

    assert(json.hcursor.downField("batch").downField("input_config").downField("file_name").succeeded)
    assertEquals(
      json.hcursor.downField("batch").downField("input_config").downField("file_name").as[String],
      Right(fileUri)
    )
  }

  test("BatchGenerateContentRequest apply(requests) should set input field correctly") {
    val model    = GeminiConstants.DefaultModel
    val requests = List(
      GenerateContentRequest(
        model,
        List(Content(List(ContentPart.Text("Test"))))
      )
    )
    val req      = BatchGenerateContentRequest(requests)

    assertEquals(req.input, BatchInput.InlineRequests(requests))
  }

  test("BatchGenerateContentRequest apply(dataset) should set input field correctly") {
    val dataset = "gs://my-bucket/data.jsonl"
    val req     = BatchGenerateContentRequest(dataset)

    assertEquals(req.input, BatchInput.FileDataset(dataset))
  }
}
