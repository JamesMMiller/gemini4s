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
    val req  = BatchGenerateContentRequest(BatchInput.GcsFile(GcsUri("gs://bucket/path/file.jsonl")))
    val json = req.asJson

    assert(json.hcursor.downField("batch").downField("input_config").downField("gcs_source").downField("uri").succeeded)
    assertEquals(
      json.hcursor.downField("batch").downField("input_config").downField("gcs_source").downField("uri").as[String],
      Right("gs://bucket/path/file.jsonl")
    )
  }

  test("BatchGenerateContentRequest encoder should handle file URI") {
    val fileResourceName = "files/abc123"
    val req              = BatchGenerateContentRequest(BatchInput.ApiFile(ResourceName(fileResourceName)))
    val json             = req.asJson

    assert(json.hcursor.downField("batch").downField("input_config").downField("file_name").succeeded)
    assertEquals(
      json.hcursor.downField("batch").downField("input_config").downField("file_name").as[String],
      Right(fileResourceName)
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

  test("BatchGenerateContentRequest apply(dataset) should set GCS input correctly") {
    val dataset = "gs://my-bucket/data.jsonl"
    val req     = BatchGenerateContentRequest(BatchInput.GcsFile(GcsUri(dataset)))

    assertEquals(req.input, BatchInput.GcsFile(GcsUri(dataset)))
  }

  test("BatchGenerateContentRequest apply(dataset) should set API File input correctly") {
    val dataset = "files/abc123"
    val req     = BatchGenerateContentRequest(BatchInput.ApiFile(ResourceName(dataset)))

    assertEquals(req.input, BatchInput.ApiFile(ResourceName(dataset)))
  }

  test("BatchGenerateContentRequest decoder should decode GCS URI") {
    val jsonString = """{"batch": {"input_config": {"gcs_source": {"uri": "gs://bucket/file.jsonl"}}}}"""
    val decoded    = decode[BatchGenerateContentRequest](jsonString)

    assertEquals(decoded, Right(BatchGenerateContentRequest(BatchInput.GcsFile(GcsUri("gs://bucket/file.jsonl")))))
  }

  test("BatchGenerateContentRequest decoder should decode file name URI") {
    val jsonString = """{"batch": {"input_config": {"file_name": "files/abc123"}}}"""
    val decoded    = decode[BatchGenerateContentRequest](jsonString)

    assertEquals(decoded, Right(BatchGenerateContentRequest(BatchInput.ApiFile(ResourceName("files/abc123")))))
  }

  test("BatchGenerateContentRequest decoder should fail for inline requests (currently unsupported)") {
    val jsonString = """{"batch": {"input_config": {"requests": {"requests": []}}}}"""
    val decoded    = decode[BatchGenerateContentRequest](jsonString)

    assert(decoded.isLeft)
    assert(decoded.left.exists(_.getMessage.contains("Decoding inline requests is not currently supported")))
  }
}
