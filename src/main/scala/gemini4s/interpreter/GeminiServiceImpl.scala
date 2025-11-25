package gemini4s.interpreter

import cats.effect.Async
import cats.syntax.all._
import fs2.Stream

import gemini4s.GeminiService
import gemini4s.error.GeminiError
import gemini4s.http.GeminiHttpClient
import gemini4s.model.domain._
import gemini4s.model.request._
import gemini4s.model.response._

/**
 * Live implementation of the GeminiService using Cats Effect.
 * Uses GeminiHttpClient for API communication.
 */
final class GeminiServiceImpl[F[_]: Async](
    httpClient: GeminiHttpClient[F]
) extends GeminiService[F] {

  override def generateContent(
      request: GenerateContentRequest
  ): F[Either[GeminiError, GenerateContentResponse]] = httpClient.post[GenerateContentRequest, GenerateContentResponse](
    GeminiConstants.Endpoints.generateContent(request.model),
    request
  )

  override def generateContentStream(
      request: GenerateContentRequest
  ): Stream[F, GenerateContentResponse] = httpClient.postStream[GenerateContentRequest, GenerateContentResponse](
    GeminiConstants.Endpoints.generateContentStream(request.model),
    request
  )

  override def countTokens(
      request: CountTokensRequest
  ): F[Either[GeminiError, Int]] = httpClient
    .post[CountTokensRequest, CountTokensResponse](
      GeminiConstants.Endpoints.countTokens(request.model),
      request
    )
    .map(_.map(_.totalTokens))

  override def embedContent(
      request: EmbedContentRequest
  ): F[Either[GeminiError, ContentEmbedding]] = httpClient
    .post[EmbedContentRequest, EmbedContentResponse](
      GeminiConstants.Endpoints.embedContent(request.model),
      request
    )
    .map(_.map(_.embedding))

  override def batchEmbedContents(
      request: BatchEmbedContentsRequest
  ): F[Either[GeminiError, List[ContentEmbedding]]] = httpClient
    .post[BatchEmbedContentsRequest, BatchEmbedContentsResponse](
      GeminiConstants.Endpoints.batchEmbedContents(request.model),
      request
    )
    .map(_.map(_.embeddings))

  override def createCachedContent(
      request: CreateCachedContentRequest
  ): F[Either[GeminiError, CachedContent]] = httpClient.post[CreateCachedContentRequest, CachedContent](
    GeminiConstants.Endpoints.createCachedContent,
    request
  )

}

object GeminiServiceImpl {

  /**
   * Creates a new GeminiService instance.
   */
  def make[F[_]: Async](
      httpClient: GeminiHttpClient[F]
  ): GeminiService[F] = new GeminiServiceImpl(httpClient)

}
