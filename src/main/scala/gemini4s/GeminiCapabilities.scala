package gemini4s

import cats.effect.Async
import fs2.Stream

import gemini4s.error.GeminiError
import gemini4s.model.domain._
import gemini4s.model.request._
import gemini4s.model.response._

/**
 * Core Gemini API capabilities available in all API versions.
 *
 * This trait provides the fundamental operations that work with both v1 and v1beta:
 * - Content generation (text, multimodal)
 * - Streaming generation
 * - Token counting
 * - Embeddings
 *
 * @tparam F The effect type (e.g., IO for Cats Effect)
 */
trait GeminiCore[F[_]] {

  /**
   * Generates content using the Gemini API.
   */
  def generateContent(
      request: GenerateContentRequest
  ): F[Either[GeminiError, GenerateContentResponse]]

  /**
   * Generates content with streaming response.
   */
  def generateContentStream(
      request: GenerateContentRequest
  ): Stream[F, GenerateContentResponse]

  /**
   * Counts tokens in the provided content.
   */
  def countTokens(
      request: CountTokensRequest
  ): F[Either[GeminiError, Int]]

  /**
   * Generates an embedding for content.
   */
  def embedContent(
      request: EmbedContentRequest
  ): F[Either[GeminiError, ContentEmbedding]]

  /**
   * Generates embeddings for a batch of contents.
   */
  def batchEmbedContents(
      request: BatchEmbedContentsRequest
  ): F[Either[GeminiError, List[ContentEmbedding]]]

}

/**
 * File API capabilities (v1beta only).
 *
 * Provides file upload, listing, and management operations.
 * These features are only available in the v1beta API.
 */
trait GeminiFiles[F[_]] {

  /**
   * Uploads a file using the resumable upload protocol.
   */
  def uploadFile(
      path: java.nio.file.Path,
      mimeType: String,
      displayName: Option[String] = None
  ): F[Either[GeminiError, File]]

  /**
   * Lists files with pagination.
   */
  def listFiles(
      pageSize: Int = 10,
      pageToken: Option[String] = None
  ): F[Either[GeminiError, ListFilesResponse]]

  /**
   * Gets a file by name.
   */
  def getFile(name: String): F[Either[GeminiError, File]]

  /**
   * Deletes a file by name.
   */
  def deleteFile(name: String): F[Either[GeminiError, Unit]]
}

/**
 * Caching capabilities (v1beta only).
 *
 * Provides cached content creation for efficient reuse.
 * These features are only available in the v1beta API.
 */
trait GeminiCaching[F[_]] {

  /**
   * Creates cached content for efficient reuse.
   */
  def createCachedContent(
      request: CreateCachedContentRequest
  ): F[Either[GeminiError, CachedContent]]

}

/**
 * Batch processing capabilities (v1beta only).
 *
 * Provides batch content generation operations.
 * These features are only available in the v1beta API.
 */
trait GeminiBatch[F[_]] {

  /**
   * Generates content for a batch of requests.
   */
  def batchGenerateContent(
      model: ModelName,
      requests: List[GenerateContentRequest]
  ): F[Either[GeminiError, BatchJob]]

  /**
   * Generates content for a batch using file input.
   */
  def batchGenerateContent(
      model: ModelName,
      input: BatchInput
  ): F[Either[GeminiError, BatchJob]]

  /**
   * Gets the status of a batch job.
   */
  def getBatchJob(name: String): F[Either[GeminiError, BatchJob]]

  /**
   * Lists batch jobs.
   */
  def listBatchJobs(
      pageSize: Int = 10,
      pageToken: Option[String] = None
  ): F[Either[GeminiError, ListBatchJobsResponse]]

  /**
   * Cancels a batch job.
   */
  def cancelBatchJob(name: String): F[Either[GeminiError, Unit]]

  /**
   * Deletes a batch job.
   */
  def deleteBatchJob(name: String): F[Either[GeminiError, Unit]]
}

// ============================================
// Version-Specific Type Aliases
// ============================================

/**
 * Full Gemini service with all v1beta capabilities.
 *
 * This is the default service type when using `GeminiService.make`.
 * It includes all features: core, files, caching, and batch operations.
 */
type GeminiServiceFull[F[_]] = GeminiCore[F] & GeminiFiles[F] & GeminiCaching[F] & GeminiBatch[F]

/**
 * Minimal Gemini service with only v1 (stable) capabilities.
 *
 * Use `GeminiService.makeV1` to get this limited service type.
 * It only includes core operations: generation, streaming, counting, embeddings.
 */
type GeminiServiceV1[F[_]] = GeminiCore[F]
