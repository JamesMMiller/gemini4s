package gemini4s

import cats.effect.{ Async, Resource }
import cats.syntax.all._
import fs2.Stream
import sttp.client3.httpclient.fs2.HttpClientFs2Backend

import gemini4s.config.{ ApiKey, GeminiConfig }
import gemini4s.error.GeminiError
import gemini4s.http.GeminiHttpClient
import gemini4s.model.domain._
import gemini4s.model.request._
import gemini4s.model.response._

/**
 * Core algebra for interacting with the Google Gemini API.
 *
 * This service follows the Tagless Final pattern, allowing for different effect type implementations.
 * It provides high-level operations for content generation, streaming, and token counting.
 *
 * @tparam F The effect type (e.g., IO for Cats Effect implementation)
 */
trait GeminiService[F[_]] {

  /**
   * Generates content using the Gemini API.
   *
   * @param request The generation request
   * @return Either a [[gemini4s.error.GeminiError]] or a [[gemini4s.model.response.GenerateContentResponse]]
   */
  def generateContent(
      request: GenerateContentRequest
  ): F[Either[GeminiError, GenerateContentResponse]]

  /**
   * Generates content using the Gemini API with streaming response.
   *
   * @param request The generation request
   * @return A stream of [[gemini4s.model.response.GenerateContentResponse]] chunks
   */
  def generateContentStream(
      request: GenerateContentRequest
  ): Stream[F, GenerateContentResponse]

  /**
   * Counts tokens in the provided content.
   *
   * @param request The count tokens request
   * @return The number of tokens
   */
  def countTokens(
      request: CountTokensRequest
  ): F[Either[GeminiError, Int]]

  /**
   * Generates an embedding for the given content.
   *
   * @param request The embed content request
   * @return Either a [[gemini4s.error.GeminiError]] or the embedding values
   */
  def embedContent(
      request: EmbedContentRequest
  ): F[Either[GeminiError, ContentEmbedding]]

  /**
   * Generates embeddings for a batch of contents.
   *
   * @param request The batch embed request
   * @return Either a [[gemini4s.error.GeminiError]] or the list of embeddings
   */
  def batchEmbedContents(
      request: BatchEmbedContentsRequest
  ): F[Either[GeminiError, List[ContentEmbedding]]]

  /**
   * Creates cached content for efficient reuse.
   *
   * @param request The create cached content request
   * @return Either a [[gemini4s.error.GeminiError]] or the created [[gemini4s.model.response.CachedContent]]
   */
  def createCachedContent(
      request: CreateCachedContentRequest
  ): F[Either[GeminiError, CachedContent]]

}

object GeminiService {

  /**
   * Creates a new Gemini instance with internal resource management.
   *
   * @param config The configuration for the service
   * @return A Resource containing the GeminiService
   */
  def make[F[_]: Async](
      config: GeminiConfig
  ): Resource[F, GeminiService[F]] = HttpClientFs2Backend.resource[F]().map { backend =>
    val httpClient = GeminiHttpClient.make[F](
      backend,
      ApiKey.unsafe(config.apiKey),
      config.baseUrl
    )
    new GeminiServiceImpl(httpClient)
  }

  /**
   * Creates a new Gemini instance using an existing HTTP client.
   *
   * @param httpClient The HTTP client to use for requests
   */
  def make[F[_]: Async](
      httpClient: GeminiHttpClient[F]
  ): GeminiService[F] = new GeminiServiceImpl(httpClient)

  /**
   * Creates a Content instance from text input.
   *
   * @param text The input text
   * @return A Content instance with the text wrapped in a Part
   */
  def text(text: String): Content = Content(parts = List(ContentPart.Text(text)))

  /**
   * Creates a Content instance from an image (Base64 encoded).
   *
   * @param base64 The Base64 encoded image data
   * @param mimeType The MIME type of the image (e.g., "image/jpeg")
   * @return A Content instance with the image wrapped in a Part
   */
  def image(base64: String, mimeType: String): Content =
    Content(parts = List(ContentPart.InlineData(ContentPart.MimeType(mimeType), ContentPart.Base64Data(base64))))

  /**
   * Helper to create a Content object with a file URI.
   *
   * @param uri
   *   The URI of the file.
   * @param mimeType
   *   The MIME type of the file.
   */
  def file(uri: String, mimeType: String): Content =
    Content(parts = List(ContentPart.FileData(ContentPart.MimeType(mimeType), ContentPart.FileUri(uri))))

  private final class GeminiServiceImpl[F[_]: Async](
      httpClient: GeminiHttpClient[F]
  ) extends GeminiService[F] {

    override def generateContent(
        request: GenerateContentRequest
    ): F[Either[GeminiError, GenerateContentResponse]] =
      httpClient.post[GenerateContentRequest, GenerateContentResponse](
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

}
