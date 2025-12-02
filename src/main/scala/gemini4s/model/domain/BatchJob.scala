package gemini4s.model.domain

import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._

/**
 * Represents a batch job for asynchronous processing.
 */
final case class BatchJob(
    name: String,
    state: BatchJobState,
    createTime: String,
    updateTime: String,
    error: Option[BatchJobError] = None
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
    } yield BatchJob(name, state, createTime, updateTime, error)
  }

  given Encoder[BatchJob] = Encoder.instance { job =>
    Json.obj(
      "name"     -> Json.fromString(job.name),
      "metadata" -> Json.obj(
        "state"      -> job.state.asJson,
        "createTime" -> Json.fromString(job.createTime),
        "updateTime" -> Json.fromString(job.updateTime)
      ),
      "error"    -> job.error.asJson
    )
  }

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
