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

  /**
   * Uploads a file using the resumable upload protocol.
   *
   * @param path The path to the file
   * @param mimeType The MIME type of the file
   * @param displayName Optional display name
   * @return Either a GeminiError or the uploaded File
   */
  def uploadFile(
      path: java.nio.file.Path,
      mimeType: String,
      displayName: Option[String] = None
  ): F[Either[GeminiError, File]]

  /**
   * Lists files with pagination.
   *
   * @param pageSize The number of files to return
   * @param pageToken The page token for pagination
   * @return Either a GeminiError or the ListFilesResponse
   */
  def listFiles(
      pageSize: Int = 10,
      pageToken: Option[String] = None
  ): F[Either[GeminiError, ListFilesResponse]]

  /**
   * Gets a file by name.
   *
   * @param name The resource name of the file
   * @return Either a GeminiError or the File
   */
  def getFile(
      name: String
  ): F[Either[GeminiError, File]]

  /**
   * Deletes a file by name.
   *
   * @param name The resource name of the file
   * @return Either a GeminiError or Unit
   */
  def deleteFile(
      name: String
  ): F[Either[GeminiError, Unit]]

  /**
   * Generates content for a batch of requests.
   *
   * @param model The model to use
   * @param requests The list of generation requests
   * @return Either a GeminiError or the BatchGenerateContentResponse
   */
  def batchGenerateContent(
      model: ModelName,
      requests: List[GenerateContentRequest]
  ): F[Either[GeminiError, BatchJob]]

  /**
   * Gets the status of a batch job.
   *
   * @param name The resource name of the batch job
   * @return Either a GeminiError or the BatchJob
   */
  def getBatchJob(
      name: String
  ): F[Either[GeminiError, BatchJob]]

  /**
   * Lists batch jobs.
   *
   * @param pageSize The maximum number of batch jobs to return
   * @param pageToken A page token, received from a previous list call
   * @return Either a GeminiError or the ListBatchJobsResponse
   */
  def listBatchJobs(
      pageSize: Int = 10,
      pageToken: Option[String] = None
  ): F[Either[GeminiError, ListBatchJobsResponse]]

  /**
   * Cancels a batch job.
   *
   * @param name The resource name of the batch job
   * @return Either a GeminiError or Unit
   */
  def cancelBatchJob(
      name: String
  ): F[Either[GeminiError, Unit]]

  /**
   * Deletes a batch job.
   *
   * @param name The resource name of the batch job
   * @return Either a GeminiError or Unit
   */
  def deleteBatchJob(
      name: String
  ): F[Either[GeminiError, Unit]]

  /**
   * Generates content for a batch of requests using a file dataset.
   *
   * @param model The model to use
   * @param dataset The URI of the dataset (File API or GCS)
   * @return Either a GeminiError or the BatchJob
   */
  def batchGenerateContent(
      model: ModelName,
      dataset: String
  ): F[Either[GeminiError, BatchJob]]

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
    Content(parts = List(ContentPart.InlineData(MimeType.unsafe(mimeType), ContentPart.Base64Data(base64))))

  /**
   * Helper to create a Content object with a file URI.
   *
   * @param uri
   *   The URI of the file.
   * @param mimeType
   *   The MIME type of the file.
   */
  def file(uri: String, mimeType: String): Content =
    Content(parts = List(ContentPart.FileData(MimeType.unsafe(mimeType), FileUri(uri))))

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

    override def batchGenerateContent(
        model: ModelName,
        requests: List[GenerateContentRequest]
    ): F[Either[GeminiError, BatchJob]] = httpClient.post[BatchGenerateContentRequest, BatchJob](
      GeminiConstants.Endpoints.batchGenerateContent(model),
      BatchGenerateContentRequest(requests)
    )

    override def getBatchJob(
        name: String
    ): F[Either[GeminiError, BatchJob]] = httpClient.get[BatchJob](
      GeminiConstants.Endpoints.getBatchJob(name)
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

    override def uploadFile(
        path: java.nio.file.Path,
        mimeType: String,
        displayName: Option[String]
    ): F[Either[GeminiError, File]] = {
      val fileSize = java.nio.file.Files.size(path)
      val metadata = io.circe.Json
        .obj(
          "file" -> io.circe.Json.obj(
            "displayName" -> displayName.map(io.circe.Json.fromString).getOrElse(io.circe.Json.Null)
          )
        )
        .noSpaces

      val startHeaders = Map(
        "X-Goog-Upload-Protocol"              -> "resumable",
        "X-Goog-Upload-Command"               -> "start",
        "X-Goog-Upload-Header-Content-Length" -> fileSize.toString,
        "X-Goog-Upload-Header-Content-Type"   -> mimeType
      )

      httpClient.startResumableUpload(GeminiConstants.Endpoints.uploadFile, metadata, startHeaders).flatMap {
        case Right(uploadUrl) =>
          val uploadHeaders = Map(
            "Content-Length"        -> fileSize.toString,
            "X-Goog-Upload-Command" -> "upload, finalize",
            "X-Goog-Upload-Offset"  -> "0"
          )
          httpClient.uploadChunk(uploadUrl, path, uploadHeaders)
        case Left(error)      => Async[F].pure(Left(error))
      }
    }

    override def listFiles(
        pageSize: Int,
        pageToken: Option[String]
    ): F[Either[GeminiError, ListFilesResponse]] = {
      val params = Map("pageSize" -> pageSize.toString) ++ pageToken.map("pageToken" -> _)
      httpClient.get[ListFilesResponse](GeminiConstants.Endpoints.files, params)
    }

    override def getFile(name: String): F[Either[GeminiError, File]] = httpClient.get[File](name)

    override def deleteFile(name: String): F[Either[GeminiError, Unit]] = httpClient.delete(name)

    override def listBatchJobs(
        pageSize: Int,
        pageToken: Option[String]
    ): F[Either[GeminiError, ListBatchJobsResponse]] = {
      val params = Map("pageSize" -> pageSize.toString) ++ pageToken.map("pageToken" -> _)
      httpClient.get[ListBatchJobsResponse](GeminiConstants.Endpoints.listBatchJobs, params)
    }

    override def cancelBatchJob(name: String): F[Either[GeminiError, Unit]] =
      httpClient.post[Unit, Unit](GeminiConstants.Endpoints.cancelBatchJob(name), ())

    override def deleteBatchJob(name: String): F[Either[GeminiError, Unit]] =
      httpClient.delete(GeminiConstants.Endpoints.deleteBatchJob(name))

    override def batchGenerateContent(
        model: ModelName,
        dataset: String
    ): F[Either[GeminiError, BatchJob]] = httpClient.post[BatchGenerateContentRequest, BatchJob](
      GeminiConstants.Endpoints.batchGenerateContent(model),
      BatchGenerateContentRequest(dataset)
    )

  }

}
