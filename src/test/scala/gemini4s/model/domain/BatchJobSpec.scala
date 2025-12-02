package gemini4s.model.domain

import io.circe.parser.*
import io.circe.syntax.*
import munit.FunSuite

class BatchJobSpec extends FunSuite {

  test("BatchJobState decoder should handle JOB_STATE prefixes") {
    assertEquals(decode[BatchJobState](""""JOB_STATE_PENDING""""), Right(BatchJobState.JOB_STATE_PENDING))
    assertEquals(decode[BatchJobState](""""JOB_STATE_RUNNING""""), Right(BatchJobState.JOB_STATE_RUNNING))
    assertEquals(decode[BatchJobState](""""JOB_STATE_SUCCEEDED""""), Right(BatchJobState.JOB_STATE_SUCCEEDED))
    assertEquals(decode[BatchJobState](""""JOB_STATE_FAILED""""), Right(BatchJobState.JOB_STATE_FAILED))
    assertEquals(decode[BatchJobState](""""JOB_STATE_CANCELLED""""), Right(BatchJobState.JOB_STATE_CANCELLED))
  }

  test("BatchJobState decoder should handle BATCH_STATE prefixes") {
    assertEquals(decode[BatchJobState](""""BATCH_STATE_PENDING""""), Right(BatchJobState.JOB_STATE_PENDING))
    assertEquals(decode[BatchJobState](""""BATCH_STATE_RUNNING""""), Right(BatchJobState.JOB_STATE_RUNNING))
    assertEquals(decode[BatchJobState](""""BATCH_STATE_SUCCEEDED""""), Right(BatchJobState.JOB_STATE_SUCCEEDED))
    assertEquals(decode[BatchJobState](""""BATCH_STATE_FAILED""""), Right(BatchJobState.JOB_STATE_FAILED))
    assertEquals(decode[BatchJobState](""""BATCH_STATE_CANCELLED""""), Right(BatchJobState.JOB_STATE_CANCELLED))
  }

  test("BatchJobState decoder should handle STATE_UNSPECIFIED") {
    assertEquals(decode[BatchJobState](""""STATE_UNSPECIFIED""""), Right(BatchJobState.STATE_UNSPECIFIED))
  }

  test("BatchJobState decoder should default to STATE_UNSPECIFIED for unknown") {
    assertEquals(decode[BatchJobState](""""UNKNOWN""""), Right(BatchJobState.STATE_UNSPECIFIED))
  }

  test("BatchJobState encoder should encode correctly") {
    assertEquals(BatchJobState.JOB_STATE_PENDING.asJson.asString, Some("JOB_STATE_PENDING"))
    assertEquals(BatchJobState.JOB_STATE_RUNNING.asJson.asString, Some("JOB_STATE_RUNNING"))
    assertEquals(BatchJobState.JOB_STATE_SUCCEEDED.asJson.asString, Some("JOB_STATE_SUCCEEDED"))
    assertEquals(BatchJobState.JOB_STATE_FAILED.asJson.asString, Some("JOB_STATE_FAILED"))
    assertEquals(BatchJobState.JOB_STATE_CANCELLED.asJson.asString, Some("JOB_STATE_CANCELLED"))
  }

  test("BatchJobError encoder should encode correctly") {
    val error = BatchJobError(400, "Bad request")
    val json  = error.asJson
    assert(json.hcursor.downField("code").as[Int] == Right(400))
    assert(json.hcursor.downField("message").as[String] == Right("Bad request"))
  }

  test("BatchJobError decoder should decode correctly") {
    val json    = """{"code":500,"message":"Internal error"}"""
    val decoded = decode[BatchJobError](json)
    assert(decoded.isRight)
    assertEquals(decoded.map(_.code), Right(500))
    assertEquals(decoded.map(_.message), Right("Internal error"))
  }

  test("BatchJob decoder should handle nested metadata") {
    val json    = """{
      "name": "batch123",
      "metadata": {
        "state": "JOB_STATE_SUCCEEDED",
        "createTime": "2024-01-01T00:00:00Z",
        "updateTime": "2024-01-01T01:00:00Z"
      }
    }"""
    val decoded = decode[BatchJob](json)
    assert(decoded.isRight)
    assertEquals(decoded.map(_.name), Right("batch123"))
    assertEquals(decoded.map(_.state), Right(BatchJobState.JOB_STATE_SUCCEEDED))
  }

  test("BatchJob decoder should handle error field") {
    val json    = """{
      "name": "batch456",
      "metadata": {
        "state": "JOB_STATE_FAILED",
        "createTime": "2024-01-01T00:00:00Z",
        "updateTime": "2024-01-01T01:00:00Z"
      },
      "error": {
        "code": 500,
        "message": "Processing failed"
      }
    }"""
    val decoded = decode[BatchJob](json)
    assert(decoded.isRight)
    assert(decoded.exists(_.error.isDefined))
  }

  test("BatchJob encoder should encode correctly") {
    val job  = BatchJob(
      "batch789",
      BatchJobState.JOB_STATE_RUNNING,
      "2024-01-01T00:00:00Z",
      "2024-01-01T02:00:00Z"
    )
    val json = job.asJson
    assert(json.hcursor.downField("name").as[String] == Right("batch789"))
  }
}
