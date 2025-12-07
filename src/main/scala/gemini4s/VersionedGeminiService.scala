package gemini4s

import cats.effect.{ Async, Resource }
import fs2.Stream
import sttp.client3.httpclient.fs2.HttpClientFs2Backend

import gemini4s.config.{ ApiKey, ApiVersion, GeminiConfig }
import gemini4s.config.ApiVersion._
import gemini4s.error.GeminiError
import gemini4s.http.GeminiHttpClient
import gemini4s.model.domain._
import gemini4s.model.request._
import gemini4s.model.response._

/**
 * Version-aware Gemini service that provides compile-time guarantees
 * that version-specific features are only available with the correct API version.
 *
 * Example:
 * {{{
 * import gemini4s.VersionedGeminiService
 * import gemini4s.config.{GeminiConfig, ApiVersion}
 *
 * // Create a v1beta service with full features
 * val betaService = VersionedGeminiService.v1beta[IO](apiKey)
 *
 * // These compile because v1beta has caching and files
 * betaService.use { svc =>
 *   svc.createCachedContent(...)  // ✓ v1beta has HasCaching
 *   svc.uploadFile(...)           // ✓ v1beta has HasFiles
 * }
 *
 * // Create a v1 service (stable, limited features)
 * val stableService = VersionedGeminiService.v1[IO](apiKey)
 *
 * // This would NOT compile - v1 doesn't have caching:
 * // stableService.createCachedContent(...)  // ✗ Compile error!
 * }}}
 *
 * @tparam F The effect type
 * @tparam V The API version capability type
 */
trait VersionedGeminiService[F[_], V <: VersionCapability] {

  /** The underlying unversioned service */
  def underlying: GeminiService[F]

  // ============================================
  // Core methods available in all versions
  // ============================================

  /** Generate content (available in v1 and v1beta) */
  def generateContent(
      request: GenerateContentRequest
  ): F[Either[GeminiError, GenerateContentResponse]]

  /** Stream content generation (available in v1 and v1beta) */
  def generateContentStream(
      request: GenerateContentRequest
  ): Stream[F, GenerateContentResponse]

  /** Count tokens (available in v1 and v1beta) */
  def countTokens(
      request: CountTokensRequest
  ): F[Either[GeminiError, Int]]

  /** Embed content (available in v1 and v1beta) */
  def embedContent(
      request: EmbedContentRequest
  ): F[Either[GeminiError, ContentEmbedding]]

  /** Batch embed contents (available in v1 and v1beta) */
  def batchEmbedContents(
      request: BatchEmbedContentsRequest
  ): F[Either[GeminiError, List[ContentEmbedding]]]

}

/**
 * Extended service with caching capabilities (v1beta only).
 */
trait HasCachingOps[F[_], V <: HasCaching] { self: VersionedGeminiService[F, V] =>

  /** Create cached content (v1beta only) */
  def createCachedContent(
      request: CreateCachedContentRequest
  ): F[Either[GeminiError, CachedContent]]

}

/**
 * Extended service with file operations (v1beta only).
 */
trait HasFilesOps[F[_], V <: HasFiles] { self: VersionedGeminiService[F, V] =>

  /** Upload a file (v1beta only) */
  def uploadFile(
      path: java.nio.file.Path,
      mimeType: String,
      displayName: Option[String] = None
  ): F[Either[GeminiError, File]]

  /** List files (v1beta only) */
  def listFiles(
      pageSize: Int = 10,
      pageToken: Option[String] = None
  ): F[Either[GeminiError, ListFilesResponse]]

  /** Get a file (v1beta only) */
  def getFile(name: String): F[Either[GeminiError, File]]

  /** Delete a file (v1beta only) */
  def deleteFile(name: String): F[Either[GeminiError, Unit]]
}

object VersionedGeminiService {

  /**
   * Create a v1beta service with full capabilities.
   */
  def v1beta[F[_]: Async](apiKey: String): Resource[F, V1BetaService[F]] =
    GeminiService.make[F](GeminiConfig.v1beta(apiKey)).map(svc => new V1BetaServiceImpl[F](svc))

  /**
   * Create a v1 (stable) service with limited capabilities.
   */
  def v1[F[_]: Async](apiKey: String): Resource[F, V1Service[F]] =
    GeminiService.make[F](GeminiConfig.v1(apiKey)).map(svc => new V1ServiceImpl[F](svc))

  /**
   * Create a versioned service from existing config.
   */
  def fromConfig[F[_]: Async](config: GeminiConfig): Resource[F, VersionedGeminiService[F, ?]] =
    config.apiVersion match {
      case ApiVersion.V1Beta => v1beta[F](config.apiKey)
      case ApiVersion.V1     => v1[F](config.apiKey)
    }

  // ============================================
  // Type aliases for convenience
  // ============================================

  /** v1beta service with all capabilities */
  type V1BetaService[F[_]] = VersionedGeminiService[F, V1BetaCapabilities] & HasCachingOps[F, V1BetaCapabilities] &
    HasFilesOps[F, V1BetaCapabilities]

  /** v1 (stable) service with limited capabilities */
  type V1Service[F[_]] = VersionedGeminiService[F, V1Capabilities]

  // ============================================
  // Implementation
  // ============================================

  private class V1BetaServiceImpl[F[_]: Async](val underlying: GeminiService[F])
      extends VersionedGeminiService[F, V1BetaCapabilities]
      with HasCachingOps[F, V1BetaCapabilities]
      with HasFilesOps[F, V1BetaCapabilities] {

    // Core methods
    def generateContent(request: GenerateContentRequest)       = underlying.generateContent(request)
    def generateContentStream(request: GenerateContentRequest) = underlying.generateContentStream(request)
    def countTokens(request: CountTokensRequest)               = underlying.countTokens(request)
    def embedContent(request: EmbedContentRequest)             = underlying.embedContent(request)
    def batchEmbedContents(request: BatchEmbedContentsRequest) = underlying.batchEmbedContents(request)

    // Caching (v1beta only)
    def createCachedContent(request: CreateCachedContentRequest) = underlying.createCachedContent(request)

    // Files (v1beta only)
    def uploadFile(path: java.nio.file.Path, mimeType: String, displayName: Option[String]) =
      underlying.uploadFile(path, mimeType, displayName)

    def listFiles(pageSize: Int, pageToken: Option[String]) = underlying.listFiles(pageSize, pageToken)
    def getFile(name: String)                               = underlying.getFile(name)
    def deleteFile(name: String)                            = underlying.deleteFile(name)
  }

  private class V1ServiceImpl[F[_]: Async](val underlying: GeminiService[F])
      extends VersionedGeminiService[F, V1Capabilities] {

    // Core methods only - no caching or files in v1
    def generateContent(request: GenerateContentRequest)       = underlying.generateContent(request)
    def generateContentStream(request: GenerateContentRequest) = underlying.generateContentStream(request)
    def countTokens(request: CountTokensRequest)               = underlying.countTokens(request)
    def embedContent(request: EmbedContentRequest)             = underlying.embedContent(request)
    def batchEmbedContents(request: BatchEmbedContentsRequest) = underlying.batchEmbedContents(request)
  }

}
