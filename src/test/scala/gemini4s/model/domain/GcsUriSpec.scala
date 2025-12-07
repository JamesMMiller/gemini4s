package gemini4s.model.domain

import io.circe.parser.*
import io.circe.syntax.*
import munit.FunSuite

class GcsUriSpec extends FunSuite {

  test("GcsUri should encode to JSON string") {
    val uriStr = "gs://bucket/file.jsonl"
    val uri    = GcsUri(uriStr)
    assertEquals(uri.asJson.asString, Some(uriStr))
  }

  test("GcsUri should decode from JSON string") {
    val uriStr  = "gs://bucket/file.jsonl"
    val json    = s""""$uriStr""""
    val decoded = decode[GcsUri](json)
    assertEquals(decoded, Right(GcsUri(uriStr)))
  }

  test("GcsUri should be accessible as string via value extension method") {
    val uriStr = "gs://bucket/file.jsonl"
    val uri    = GcsUri(uriStr)
    assertEquals(uri.value, uriStr)
  }
}
