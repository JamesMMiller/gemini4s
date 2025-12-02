package gemini4s.model.response

import io.circe._
import io.circe.generic.semiauto._

import gemini4s.model.domain.BatchJob

final case class ListBatchJobsResponse(
    batchJobs: Option[List[BatchJob]],
    nextPageToken: Option[String]
)

object ListBatchJobsResponse {

  given Decoder[ListBatchJobsResponse] = Decoder.instance { c =>
    for {
      batchJobs     <- c.downField("batchJobs")
                         .as[Option[List[BatchJob]]]
                         .orElse(c.downField("batch_jobs").as[Option[List[BatchJob]]])
                         .orElse(c.downField("batches").as[Option[List[BatchJob]]])
                         .orElse(c.downField("jobs").as[Option[List[BatchJob]]])
      nextPageToken <-
        c.downField("nextPageToken").as[Option[String]].orElse(c.downField("next_page_token").as[Option[String]])
    } yield ListBatchJobsResponse(batchJobs, nextPageToken)
  }

  given Encoder[ListBatchJobsResponse] = deriveEncoder
}
