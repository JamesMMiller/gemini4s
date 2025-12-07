package gemini4s.model.request

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

import gemini4s.model.domain._

/**
 * Request to batch generate content.
 */
final case class BatchGenerateContentRequest(
    input: BatchInput
)

sealed trait BatchInput

object BatchInput {
  final case class InlineRequests(requests: List[GenerateContentRequest]) extends BatchInput
  final case class GcsFile(uri: GcsUri)                                   extends BatchInput
  final case class ApiFile(resourceName: ResourceName)                    extends BatchInput
}

object BatchGenerateContentRequest {

  def apply(requests: List[GenerateContentRequest]): BatchGenerateContentRequest =
    BatchGenerateContentRequest(BatchInput.InlineRequests(requests))

  given Encoder[BatchGenerateContentRequest] = Encoder.instance { req =>
    val inputConfig = req.input match {
      case BatchInput.InlineRequests(reqs) =>
        val requestsJson = reqs.zipWithIndex.map { case (r, i) =>
          Json.obj(
            "request"  -> r.asJson,
            "metadata" -> Json.obj(
              "key" -> Json.fromString(i.toString)
            )
          )
        }
        Json.obj(
          "requests" -> Json.obj(
            "requests" -> Json.fromValues(requestsJson)
          )
        )
      case BatchInput.GcsFile(uri)         => Json.obj("gcs_source" -> Json.obj("uri" -> uri.asJson))
      case BatchInput.ApiFile(name)        => Json.obj("file_name" -> name.asJson)
    }

    Json.obj(
      "batch" -> Json.obj(
        "input_config" -> inputConfig
      )
    )
  }

  // Best-effort decoder for testing purposes
  given Decoder[BatchGenerateContentRequest] = Decoder.instance { c =>
    val inputConfig = c.downField("batch").downField("input_config")

    // Try decoding GCS source
    inputConfig
      .downField("gcs_source")
      .downField("uri")
      .as[GcsUri]
      .map(uri => BatchGenerateContentRequest(BatchInput.GcsFile(uri)))
      .orElse {
        // Try decoding File API source
        inputConfig.downField("file_name").as[ResourceName].map { name =>
          BatchGenerateContentRequest(BatchInput.ApiFile(name))
        }
      }
      .orElse {
        // Inline requests decoding is skipped for now due to complexity of reconstructing
        // the original list from the (request + metadata) wrapper structure.
        // This is primarily a request model, so decoding is mainly for tests.
        Left(DecodingFailure("Decoding inline requests is not currently supported", c.history))
      }
  }

}
