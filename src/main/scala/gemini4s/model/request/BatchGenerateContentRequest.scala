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
  final case class FileDataset(uri: String)                               extends BatchInput
}

object BatchGenerateContentRequest {

  def apply(requests: List[GenerateContentRequest]): BatchGenerateContentRequest =
    BatchGenerateContentRequest(BatchInput.InlineRequests(requests))

  def apply(datasetUri: String): BatchGenerateContentRequest =
    BatchGenerateContentRequest(BatchInput.FileDataset(datasetUri))

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
      case BatchInput.FileDataset(uri)     =>
        if (uri.startsWith("gs://")) {
          Json.obj("gcs_source" -> Json.obj("uri" -> Json.fromString(uri)))
        } else {
          Json.obj("file_name" -> Json.fromString(uri)) // Assuming file API URI
        }
    }

    Json.obj(
      "batch" -> Json.obj(
        "input_config" -> inputConfig
      )
    )
  }

  given Decoder[BatchGenerateContentRequest] = Decoder.instance { c =>
    // Note: This decoder is a best-effort implementation since the API response structure
    // for requests might not match this exactly if we were decoding a response.
    // However, for symmetry in tests, we implement it.
    val inputConfig = c.downField("batch").downField("input_config")

    inputConfig
      .downField("requests")
      .downField("requests")
      .as[List[Json]]
      .flatMap { _ =>
        // Decoding inline requests is complex because of the wrapper structure (request + metadata)
        // For now, we error on this side if needed or implement a manual decoder if strict round-trip is required.
        // This is a simplification for the refactor.
        // A proper decoder would need to unwrap the "request" field from the internal structure.
        Left(DecodingFailure("Decoding BatchGenerateContentRequest is not fully supported yet", c.history))
      }
      .orElse {
        inputConfig.downField("gcs_source").downField("uri").as[String].map { uri =>
          BatchGenerateContentRequest(BatchInput.FileDataset(uri))
        }
      }
      .orElse {
        inputConfig.downField("file_name").as[String].map { uri =>
          BatchGenerateContentRequest(BatchInput.FileDataset(uri))
        }
      }
  }

}
