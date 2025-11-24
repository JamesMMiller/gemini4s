package gemini4s.http

import cats.effect.Async
import cats.syntax.all._
import fs2.Stream
import io.circe.{ Decoder, Encoder }
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3._
import sttp.client3.circe._
import sttp.model.Uri

import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError

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
   * @return Response model wrapped in effect F with GeminiError in error channel
   */
  def post[Req: Encoder, Res: Decoder](
      endpoint: String,
      request: Req
  ): F[Either[GeminiError, Res]]

  /**
   * Sends a POST request and streams the response.
   *
   * @param endpoint The API endpoint path
   * @param request The typed request model
   * @return Response chunks as a stream
   */
  def postStream[Req: Encoder, Res: Decoder](
      endpoint: String,
      request: Req
  ): Stream[F, Res]

}

object GeminiHttpClient {

  /**
   * Creates a Sttp-based implementation of GeminiHttpClient.
   */
  def make[F[_]: Async](
      backend: SttpBackend[F, Fs2Streams[F]],
      config: GeminiConfig
  ): GeminiHttpClient[F] = new GeminiHttpClient[F] {

    override def post[Req: Encoder, Res: Decoder](
        endpoint: String,
        request: Req
    ): F[Either[GeminiError, Res]] = {
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

    override def postStream[Req: Encoder, Res: Decoder](
        endpoint: String,
        request: Req
    ): Stream[F, Res] = {
      val uri = uri"${config.baseUrl}".addPath(endpoint.split('/')).addParam("key", config.apiKey)

      val req = basicRequest.post(uri).body(request).response(asStreamUnsafe(Fs2Streams[F]))

      Stream
        .eval(backend.send(req))
        .flatMap { response =>
          response.body match {
            case Right(stream)   => stream
                .through(fs2.text.utf8.decode)
                .through(io.circe.fs2.stringArrayParser)
                .through(io.circe.fs2.decoder[F, Res])
            case Left(errorBody) =>
              Stream.raiseError(GeminiError.InvalidRequest(s"API error: ${response.code} - $errorBody", None))
          }
        }
        .handleErrorWith {
          case e: GeminiError => Stream.raiseError(e)
          case e              => Stream.raiseError(GeminiError.ConnectionError(e.getMessage, Some(e)))
        }
    }

  }

}
