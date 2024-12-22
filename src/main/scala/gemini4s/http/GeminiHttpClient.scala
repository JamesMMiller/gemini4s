package gemini4s.http

import zio.stream.ZStream

import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError
import gemini4s.model.{GeminiRequest, GeminiResponse}

/**
 * HTTP client algebra for Gemini API.
 * Uses tagless final pattern to allow different implementations.
 *
 * @tparam F The effect type
 */
trait GeminiHttpClient[F[_]] {
  /**
   * Sends a POST request to the Gemini API.
   *
   * @param endpoint The API endpoint path
   * @param request The typed request model
   * @param config The API configuration
   * @return Response model wrapped in effect F with GeminiError in error channel
   */
  def post[Req <: GeminiRequest, Res <: GeminiResponse](
    endpoint: String,
    request: Req
  )(using config: GeminiConfig): F[Either[GeminiError, Res]]

  /**
   * Sends a POST request and streams the response.
   *
   * @param endpoint The API endpoint path
   * @param request The typed request model
   * @param config The API configuration
   * @return Response chunks as a stream with GeminiError in error channel
   */
  def postStream[Req <: GeminiRequest, Res <: GeminiResponse](
    endpoint: String,
    request: Req
  )(using config: GeminiConfig): F[ZStream[Any, GeminiError, Res]]
} 