package gemini4s.model.response

import io.circe._
import io.circe.generic.semiauto._

/**
 * Response from token counting.
 */
final case class CountTokensResponse(
    totalTokens: Int,
    cachedContentTokenCount: Option[Int] = None,
    candidatesTokenCount: Option[Int] = None
)

object CountTokensResponse {

  given Decoder[CountTokensResponse] = (c: HCursor) =>
    for {
      totalTokens             <- c.downField("totalTokens").as[Int]
      cachedContentTokenCount <- c.downField("cachedContentTokenCount").as[Option[Int]]
      candidatesTokenCount    <- c.downField("candidatesTokenCount").as[Option[Int]]
    } yield CountTokensResponse(totalTokens, cachedContentTokenCount, candidatesTokenCount)

  given Encoder[CountTokensResponse] = deriveEncoder
}
