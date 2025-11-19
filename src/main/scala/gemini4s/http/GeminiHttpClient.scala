package gemini4s.http

import cats.effect.Async
import cats.syntax.all._
import fs2.Stream
import io.circe.{ Decoder, Encoder }
import sttp.client3._
import sttp.client3.circe._
import sttp.model.Uri

import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError
import gemini4s.model.GeminiRequest

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
  def post[Req <: GeminiRequest: Encoder, Res: Decoder](
      endpoint: String,
      request: Req
  )(using config: GeminiConfig): F[Either[GeminiError, Res]]

  /**
   * Sends a POST request and streams the response.
   *
   * @param endpoint The API endpoint path
   * @param request The typed request model
   * @param config The API configuration
   * @return Response chunks as a stream
   */
  def postStream[Req <: GeminiRequest: Encoder, Res: Decoder](
      endpoint: String,
      request: Req
  )(using config: GeminiConfig): Stream[F, Res]

}

object GeminiHttpClient {

  /**
   * Creates a Sttp-based implementation of GeminiHttpClient.
   */
  def make[F[_]: Async](
      backend: SttpBackend[F, Any]
  ): GeminiHttpClient[F] = new GeminiHttpClient[F] {

    override def post[Req <: GeminiRequest: Encoder, Res: Decoder](
        endpoint: String,
        request: Req
    )(using config: GeminiConfig): F[Either[GeminiError, Res]] = {
      val uri = uri"${config.baseUrl}".addPath(endpoint.split('/')).addParam("key", config.apiKey)

      basicRequest
        .post(uri)
        .body(request)
        .response(asJson[Res])
        .send(backend)
        .map { response =>
          response.body match {
            case Right(success) => Right(success)
            case Left(error)    =>
              Left(GeminiError.InvalidRequest(s"API error: ${response.code} - ${error.getMessage}", None))
          }
        }
        .handleError(error => Left(GeminiError.ConnectionError(error.getMessage, Some(error))))
    }

    override def postStream[Req <: GeminiRequest: Encoder, Res: Decoder](
        endpoint: String,
        request: Req
    )(using config: GeminiConfig): Stream[F, Res] =
      // Note: Streaming implementation requires a streaming-capable backend (like fs2 backend)
      // For simplicity in this interface, we might need to adjust how we expose streaming
      // or assume the backend handles it.
      // With sttp, streaming response handling is backend-specific.
      // Here we assume a standard request for now as a placeholder or need to use stream-specific request

      // To properly support streaming with sttp and fs2, we need to use `asStream` response specification
      // which requires passing the streams capability.
      // Since we want to be generic, this is tricky without more context on the backend capabilities.
      // However, for a "state of the art" library, we should probably require `SttpBackend[F, fs2.Stream[F, Byte]]`.

      Stream.raiseError(new NotImplementedError("Streaming not yet fully implemented for generic backend"))

  }

}
