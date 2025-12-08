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
 * GeminiService is the main entry point for the Gemini API.
 *
 * It's a type alias for the full v1beta service with all capabilities:
 * - Core (generation, streaming, counting, embeddings)
 * - Files (upload, list, get, delete)
 * - Caching (create cached content)
 * - Batch (batch generation operations)
 *
 * For the limited v1 API, use [[GeminiServiceV1]] via [[GeminiService.makeV1]].
 *
 * @example
 * {{{
 * import gemini4s.GeminiService
 *
 * // Full v1beta service (default)
 * GeminiService.make[IO](config).use { svc =>
 *   svc.generateContent(...)  // Available
 *   svc.uploadFile(...)       // Available
 *   svc.createCachedContent(...) // Available
 * }
 *
 * // Minimal v1 service
 * GeminiService.makeV1[IO](apiKey).use { svc =>
 *   svc.generateContent(...)  // Available
 *   // svc.uploadFile(...)    // Not available - compile error!
 * }
 * }}}
 */
type GeminiService[F[_]] = GeminiServiceFull[F]

object GeminiService {

  /**
   * Creates a full Gemini service with all v1beta capabilities.
   *
   * @param config The configuration (uses v1beta by default)
   * @return A Resource containing the full GeminiService
   */
  def make[F[_]: Async](
      config: GeminiConfig
  ): Resource[F, GeminiService[F]] = HttpClientFs2Backend.resource[F]().map { backend =>
    val httpClient = GeminiHttpClient.make[F](
      backend,
      ApiKey.unsafe(config.apiKey),
      config.versionedBaseUrl
    )
    new GeminiServiceImpl(httpClient)
  }

  /**
   * Creates a full Gemini service using an existing HTTP client.
   *
   * @param httpClient The HTTP client to use
   */
  def make[F[_]: Async](
      httpClient: GeminiHttpClient[F]
  ): GeminiService[F] = new GeminiServiceImpl(httpClient)

  /**
   * Creates a minimal v1 Gemini service with only core capabilities.
   *
   * This service does NOT have file, caching, or batch operations.
   * Use this when you only need basic generation and embedding.
   *
   * @param apiKey Your Gemini API key
   * @return A Resource containing the v1 service
   */
  def makeV1[F[_]: Async](
      apiKey: String
  ): Resource[F, GeminiServiceV1[F]] = {
    val config = GeminiConfig.v1(apiKey)
    HttpClientFs2Backend.resource[F]().map { backend =>
      val httpClient = GeminiHttpClient.make[F](
        backend,
        ApiKey.unsafe(config.apiKey),
        config.versionedBaseUrl
      )
      new GeminiCoreImpl(httpClient)
    }
  }

  // ============================================
  // Content Helpers
  // ============================================

  /**
   * Creates a Content instance from text input.
   */
  def text(text: String): Content = Content(parts = List(ContentPart.Text(text)))

  /**
   * Creates a Content instance from an image (Base64 encoded).
   */
  def image(base64: String, mimeType: String): Content =
    Content(parts = List(ContentPart.InlineData(MimeType.unsafe(mimeType), ContentPart.Base64Data(base64))))

  /**
   * Creates a Content instance with a file URI.
   */
  def file(uri: String, mimeType: String): Content =
    Content(parts = List(ContentPart.FileData(MimeType.unsafe(mimeType), FileUri(uri))))

  // ============================================
  // Type-Safe Model Capability Extensions
  // ============================================

  import gemini4s.model.domain.ModelCapabilities._

  /**
   * Extension methods for type-safe model capability checking.
   */
  object ops {

    extension [F[_]](service: GeminiCore[F]) {

      /**
       * Type-safe content generation - requires model with CanGenerate capability.
       */
      def generateWithModel[M: SupportsGeneration](
          model: M,
          contents: List[Content],
          config: Option[GenerationConfig] = None,
          safetySettings: Option[List[SafetySetting]] = None,
          systemInstruction: Option[Content] = None
      ): F[Either[GeminiError, GenerateContentResponse]] = {
        val modelName = summon[SupportsGeneration[M]].toModelName(model)
        service.generateContent(
          GenerateContentRequest(modelName, contents, safetySettings, config, systemInstruction)
        )
      }

      /**
       * Type-safe streaming generation - requires model with CanGenerate capability.
       */
      def streamWithModel[M: SupportsGeneration](
          model: M,
          contents: List[Content],
          config: Option[GenerationConfig] = None,
          safetySettings: Option[List[SafetySetting]] = None,
          systemInstruction: Option[Content] = None
      ): Stream[F, GenerateContentResponse] = {
        val modelName = summon[SupportsGeneration[M]].toModelName(model)
        service.generateContentStream(
          GenerateContentRequest(modelName, contents, safetySettings, config, systemInstruction)
        )
      }

      /**
       * Type-safe embedding - requires model with CanEmbed capability.
       */
      def embedWithModel[M: SupportsEmbedding](
          model: M,
          content: Content,
          taskType: Option[TaskType] = None,
          title: Option[String] = None
      ): F[Either[GeminiError, ContentEmbedding]] = {
        val modelName = summon[SupportsEmbedding[M]].toModelName(model)
        service.embedContent(EmbedContentRequest(content, modelName, taskType, title))
      }

      /**
       * Type-safe batch embedding - requires model with CanEmbed capability.
       */
      def batchEmbedWithModel[M: SupportsEmbedding](
          model: M,
          requests: List[EmbedContentRequest]
      ): F[Either[GeminiError, List[ContentEmbedding]]] = {
        val modelName = summon[SupportsEmbedding[M]].toModelName(model)
        service.batchEmbedContents(BatchEmbedContentsRequest(modelName, requests))
      }

      /**
       * Type-safe token counting - requires model with CanCount capability.
       */
      def countWithModel[M: SupportsTokenCount](
          model: M,
          contents: List[Content]
      ): F[Either[GeminiError, Int]] = {
        val modelName = summon[SupportsTokenCount[M]].toModelName(model)
        service.countTokens(CountTokensRequest(modelName, contents))
      }

    }

    extension [F[_]](service: GeminiCaching[F]) {

      /**
       * Type-safe cached content creation - requires model with CanCache capability.
       */
      def createCacheWithModel[M: SupportsCaching](
          model: M,
          contents: List[Content],
          displayName: Option[String] = None,
          ttl: Option[String] = None,
          systemInstruction: Option[Content] = None
      ): F[Either[GeminiError, CachedContent]] = {
        val modelName = summon[SupportsCaching[M]].toModelName(model)
        service.createCachedContent(
          CreateCachedContentRequest(
            model = Some(modelName.value),
            contents = Some(contents),
            displayName = displayName,
            ttl = ttl,
            systemInstruction = systemInstruction
          )
        )
      }

    }

    extension [F[_]](service: GeminiBatch[F]) {

      /**
       * Type-safe batch generation - requires model with CanBatch capability.
       */
      def batchGenerateWithModel[M: SupportsBatch](
          model: M,
          requests: List[GenerateContentRequest]
      )(using Async[F]): F[Either[GeminiError, BatchJob]] = {
        val modelName = summon[SupportsBatch[M]].toModelName(model)
        service.batchGenerateContent(modelName, requests)
      }

    }

  }

  // ============================================
  // Implementations
  // ============================================

  /**
   * Core-only implementation for v1 API.
   */
  private final class GeminiCoreImpl[F[_]: Async](
      httpClient: GeminiHttpClient[F]
  ) extends GeminiCore[F] {

    def generateContent(request: GenerateContentRequest): F[Either[GeminiError, GenerateContentResponse]] =
      httpClient.post[GenerateContentRequest, GenerateContentResponse](
        GeminiConstants.Endpoints.generateContent(request.model),
        request
      )

    def generateContentStream(request: GenerateContentRequest): Stream[F, GenerateContentResponse] =
      httpClient.postStream[GenerateContentRequest, GenerateContentResponse](
        GeminiConstants.Endpoints.generateContentStream(request.model),
        request
      )

    def countTokens(request: CountTokensRequest): F[Either[GeminiError, Int]] = httpClient
      .post[CountTokensRequest, CountTokensResponse](
        GeminiConstants.Endpoints.countTokens(request.model),
        request
      )
      .map(_.map(_.totalTokens))

    def embedContent(request: EmbedContentRequest): F[Either[GeminiError, ContentEmbedding]] = httpClient
      .post[EmbedContentRequest, EmbedContentResponse](
        GeminiConstants.Endpoints.embedContent(request.model),
        request
      )
      .map(_.map(_.embedding))

    def batchEmbedContents(request: BatchEmbedContentsRequest): F[Either[GeminiError, List[ContentEmbedding]]] =
      httpClient
        .post[BatchEmbedContentsRequest, BatchEmbedContentsResponse](
          GeminiConstants.Endpoints.batchEmbedContents(request.model),
          request
        )
        .map(_.map(_.embeddings))

  }

  /**
   * Full implementation for v1beta API with all capabilities.
   */
  private final class GeminiServiceImpl[F[_]: Async](
      httpClient: GeminiHttpClient[F]
  ) extends GeminiCore[F]
      with GeminiFiles[F]
      with GeminiCaching[F]
      with GeminiBatch[F] {

    // Core
    def generateContent(request: GenerateContentRequest): F[Either[GeminiError, GenerateContentResponse]] =
      httpClient.post[GenerateContentRequest, GenerateContentResponse](
        GeminiConstants.Endpoints.generateContent(request.model),
        request
      )

    def generateContentStream(request: GenerateContentRequest): Stream[F, GenerateContentResponse] =
      httpClient.postStream[GenerateContentRequest, GenerateContentResponse](
        GeminiConstants.Endpoints.generateContentStream(request.model),
        request
      )

    def countTokens(request: CountTokensRequest): F[Either[GeminiError, Int]] = httpClient
      .post[CountTokensRequest, CountTokensResponse](
        GeminiConstants.Endpoints.countTokens(request.model),
        request
      )
      .map(_.map(_.totalTokens))

    def embedContent(request: EmbedContentRequest): F[Either[GeminiError, ContentEmbedding]] = httpClient
      .post[EmbedContentRequest, EmbedContentResponse](
        GeminiConstants.Endpoints.embedContent(request.model),
        request
      )
      .map(_.map(_.embedding))

    def batchEmbedContents(request: BatchEmbedContentsRequest): F[Either[GeminiError, List[ContentEmbedding]]] =
      httpClient
        .post[BatchEmbedContentsRequest, BatchEmbedContentsResponse](
          GeminiConstants.Endpoints.batchEmbedContents(request.model),
          request
        )
        .map(_.map(_.embeddings))

    // Caching
    def createCachedContent(request: CreateCachedContentRequest): F[Either[GeminiError, CachedContent]] =
      httpClient.post[CreateCachedContentRequest, CachedContent](
        GeminiConstants.Endpoints.createCachedContent,
        request
      )

    // Files
    def uploadFile(
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

      httpClient.startResumableUpload(GeminiConstants.Endpoints.uploadFile(), metadata, startHeaders).flatMap {
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

    def listFiles(pageSize: Int, pageToken: Option[String]): F[Either[GeminiError, ListFilesResponse]] = {
      val params = Map("pageSize" -> pageSize.toString) ++ pageToken.map("pageToken" -> _)
      httpClient.get[ListFilesResponse](GeminiConstants.Endpoints.files, params)
    }

    def getFile(name: String): F[Either[GeminiError, File]] = httpClient.get[File](name)

    def deleteFile(name: String): F[Either[GeminiError, Unit]] = httpClient.delete(name)

    // Batch
    def batchGenerateContent(
        model: ModelName,
        requests: List[GenerateContentRequest]
    ): F[Either[GeminiError, BatchJob]] = httpClient.post[BatchGenerateContentRequest, BatchJob](
      GeminiConstants.Endpoints.batchGenerateContent(model),
      BatchGenerateContentRequest(requests)
    )

    def batchGenerateContent(model: ModelName, input: BatchInput): F[Either[GeminiError, BatchJob]] =
      httpClient.post[BatchGenerateContentRequest, BatchJob](
        GeminiConstants.Endpoints.batchGenerateContent(model),
        BatchGenerateContentRequest(input)
      )

    def getBatchJob(name: String): F[Either[GeminiError, BatchJob]] =
      httpClient.get[BatchJob](GeminiConstants.Endpoints.getBatchJob(name))

    def listBatchJobs(pageSize: Int, pageToken: Option[String]): F[Either[GeminiError, ListBatchJobsResponse]] = {
      val params = Map("pageSize" -> pageSize.toString) ++ pageToken.map("pageToken" -> _)
      httpClient.get[ListBatchJobsResponse](GeminiConstants.Endpoints.listBatchJobs, params)
    }

    def cancelBatchJob(name: String): F[Either[GeminiError, Unit]] =
      httpClient.post[Unit, Unit](GeminiConstants.Endpoints.cancelBatchJob(name), ())

    def deleteBatchJob(name: String): F[Either[GeminiError, Unit]] =
      httpClient.delete(GeminiConstants.Endpoints.deleteBatchJob(name))

  }

}
