package gemini4s

import fs2.Stream

import gemini4s.error.GeminiError
import gemini4s.model.domain._
import gemini4s.model.request._
import gemini4s.model.response._

/**
 * Core service algebra for interacting with the Google Gemini API.
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
   * Creates a Content instance from text input.
   *
   * @param text The input text
   * @return A Content instance with the text wrapped in a Part
   */
  def text(text: String): Content = Content(parts = List(ContentPart(text = text)))

}
