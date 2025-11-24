package gemini4s.model.domain

import io.circe._

/**
 * Type of task for which the embedding will be used.
 */
enum TaskType {
  case TASK_TYPE_UNSPECIFIED, RETRIEVAL_QUERY, RETRIEVAL_DOCUMENT, SEMANTIC_SIMILARITY, CLASSIFICATION, CLUSTERING
}

object TaskType {
  given Encoder[TaskType] = Encoder.encodeString.contramap(_.toString)

  given Decoder[TaskType] = Decoder.decodeString.emap { str =>
    try Right(TaskType.valueOf(str))
    catch { case _: IllegalArgumentException => Left(s"Unknown TaskType: $str") }
  }

}
