package gemini4s.model.request

import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._

import gemini4s.model.domain._

/**
 * Request to batch generate content.
 */
final case class BatchGenerateContentRequest(
    requests: Option[List[GenerateContentRequest]] = None,
    dataset: Option[String] = None
)

object BatchGenerateContentRequest {

  def apply(requests: List[GenerateContentRequest]): BatchGenerateContentRequest =
    BatchGenerateContentRequest(Some(requests), None)

  def apply(dataset: String): BatchGenerateContentRequest = BatchGenerateContentRequest(None, Some(dataset))

  given Encoder[BatchGenerateContentRequest] = Encoder.instance { req =>
    val inputConfig = (req.requests, req.dataset) match {
      case (Some(reqs), _) =>
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
      case (_, Some(uri))  =>
        if (uri.startsWith("gs://")) {
          Json.obj("gcs_source" -> Json.obj("uri" -> Json.fromString(uri)))
        } else {
          Json.obj("file_name" -> Json.fromString(uri))
        }
      case (None, None)    => Json.obj() // Should not happen if at least one is provided
    }

    Json.obj(
      "batch" -> Json.obj(
        "input_config" -> inputConfig
      )
    )
  }

  given Decoder[BatchGenerateContentRequest] = deriveDecoder
}
