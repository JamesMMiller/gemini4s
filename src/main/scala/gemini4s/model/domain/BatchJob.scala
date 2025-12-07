package gemini4s.model.domain

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

import gemini4s.model.response.GenerateContentResponse

/**
 * Represents a batch job for asynchronous processing.
 */
final case class BatchJob(
    name: String,
    state: BatchJobState,
    createTime: String,
    updateTime: String,
    error: Option[BatchJobError] = None,
    response: Option[BatchJobResponse] = None
)

object BatchJob {

  given Decoder[BatchJob] = Decoder.instance { c =>
    for {
      name       <- c.downField("name").as[String]
      metadata    = c.downField("metadata")
      state      <- metadata.downField("state").as[BatchJobState]
      createTime <- metadata.downField("createTime").as[String]
      updateTime <- metadata.downField("updateTime").as[String]
      error      <- c.downField("error").as[Option[BatchJobError]]
      response   <- c.downField("response").as[Option[BatchJobResponse]]
    } yield BatchJob(name, state, createTime, updateTime, error, response)
  }

  given Encoder[BatchJob] = Encoder.instance { job =>
    Json.obj(
      "name"     -> Json.fromString(job.name),
      "metadata" -> Json.obj(
        "state"      -> job.state.asJson,
        "createTime" -> Json.fromString(job.createTime),
        "updateTime" -> Json.fromString(job.updateTime)
      ),
      "error"    -> job.error.asJson,
      "response" -> job.response.asJson
    )
  }

}

/**
 * Response from a batch job containing results.
 */
final case class BatchJobResponse(
    inlinedResponses: Option[List[BatchInlineResponse]] = None,
    responsesFile: Option[String] = None
)

object BatchJobResponse {

  given Decoder[BatchJobResponse] = Decoder.instance { c =>
    val inlinedResponsesCursor = c.downField("inlinedResponses")

    // Check if inlinedResponses is an array (standard) or object (nested)
    val inlinedResponsesDecoder: Decoder[Option[List[BatchInlineResponse]]] =
      Decoder.decodeOption(Decoder.decodeList[BatchInlineResponse])

    val inlinedResponsesResult = inlinedResponsesDecoder.tryDecode(inlinedResponsesCursor).orElse {
      // If direct decoding fails, try one level deeper (handling the case where it's wrapped in an object)
      inlinedResponsesCursor.downField("inlinedResponses").as[Option[List[BatchInlineResponse]]]
    }

    for {
      inlined       <- inlinedResponsesResult
      responsesFile <- c.downField("responsesFile").as[Option[String]]
    } yield BatchJobResponse(inlined, responsesFile)
  }

  given Encoder[BatchJobResponse] = deriveEncoder
}

/**
 * Individual response within an inlined batch result.
 */
final case class BatchInlineResponse(
    response: Option[GenerateContentResponse] = None,
    error: Option[BatchJobError] = None
)

object BatchInlineResponse {
  given Decoder[BatchInlineResponse] = deriveDecoder
  given Encoder[BatchInlineResponse] = deriveEncoder
}

/**
 * State of a batch job.
 */
enum BatchJobState {
  case STATE_UNSPECIFIED
  case JOB_STATE_PENDING
  case JOB_STATE_RUNNING
  case JOB_STATE_SUCCEEDED
  case JOB_STATE_FAILED
  case JOB_STATE_CANCELLED
}

object BatchJobState {

  given Decoder[BatchJobState] = Decoder.decodeString.map {
    case "STATE_UNSPECIFIED"                             => STATE_UNSPECIFIED
    case "JOB_STATE_PENDING" | "BATCH_STATE_PENDING"     => JOB_STATE_PENDING
    case "JOB_STATE_RUNNING" | "BATCH_STATE_RUNNING"     => JOB_STATE_RUNNING
    case "JOB_STATE_SUCCEEDED" | "BATCH_STATE_SUCCEEDED" => JOB_STATE_SUCCEEDED
    case "JOB_STATE_FAILED" | "BATCH_STATE_FAILED"       => JOB_STATE_FAILED
    case "JOB_STATE_CANCELLED" | "BATCH_STATE_CANCELLED" => JOB_STATE_CANCELLED
    case _                                               => STATE_UNSPECIFIED
  }

  given Encoder[BatchJobState] = Encoder.encodeString.contramap(_.toString)
}

/**
 * Error details for a failed batch job.
 */
final case class BatchJobError(
    code: Int,
    message: String
)

object BatchJobError {
  given Decoder[BatchJobError] = deriveDecoder
  given Encoder[BatchJobError] = deriveEncoder
}
