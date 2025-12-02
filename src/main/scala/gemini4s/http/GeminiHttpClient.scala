package gemini4s.http

import cats.effect.Async
import cats.syntax.all._
import fs2.Stream
import io.circe.{ Decoder, Encoder }
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3._
import sttp.client3.circe._
import sttp.model.Uri

import gemini4s.config.ApiKey
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

  /**
   * Starts a resumable upload session.
   *
   * @param uri The upload endpoint URI
   * @param metadata The file metadata as JSON string
   * @param headers Additional headers for the request
   * @return The upload URL wrapped in effect F
   */
  def startResumableUpload(
      uri: String,
      metadata: String,
      headers: Map[String, String]
  ): F[Either[GeminiError, String]]

  /**
   * Uploads a file chunk (or the whole file) to the upload URL.
   *
   * @param uploadUri The upload URL obtained from startResumableUpload
   * @param file The file to upload
   * @param headers Additional headers for the request
   * @return The uploaded File model wrapped in effect F
   */
  def uploadChunk(
      uploadUri: String,
      file: java.nio.file.Path,
      headers: Map[String, String]
  ): F[Either[GeminiError, gemini4s.model.domain.File]]

  /**
   * Sends a GET request to the Gemini API.
   *
   * @param endpoint The API endpoint path
   * @param params Query parameters
   * @return Response model wrapped in effect F
   */
  def get[Res: Decoder](
      endpoint: String,
      params: Map[String, String] = Map.empty
  ): F[Either[GeminiError, Res]]

  /**
   * Sends a DELETE request to the Gemini API.
   *
   * @param endpoint The API endpoint path
   * @return Unit wrapped in effect F
   */
  def delete(
      endpoint: String
  ): F[Either[GeminiError, Unit]]

}

object GeminiHttpClient {

  /** Default base URL for the Gemini API */
  val DefaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta"

  /**
   * Creates a Sttp-based implementation of GeminiHttpClient.
   *
   * @param backend The Sttp backend
   * @param apiKey The Gemini API key
   * @param baseUrl The base URL for API requests (defaults to official Gemini API)
   */
  def make[F[_]: Async](
      backend: SttpBackend[F, Fs2Streams[F]],
      apiKey: ApiKey,
      baseUrl: String = DefaultBaseUrl
  ): GeminiHttpClient[F] = new GeminiHttpClient[F] {

    override def post[Req: Encoder, Res: Decoder](
        endpoint: String,
        request: Req
    ): F[Either[GeminiError, Res]] = {
      val uri = uri"$baseUrl".addPath(endpoint.split('/')).addParam("key", apiKey.value)

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
      val uri = uri"$baseUrl".addPath(endpoint.split('/')).addParam("key", apiKey.value)

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

    override def get[Res: Decoder](
        endpoint: String,
        params: Map[String, String]
    ): F[Either[GeminiError, Res]] = {
      val uri           = uri"$baseUrl".addPath(endpoint.split('/')).addParam("key", apiKey.value)
      val uriWithParams = params.foldLeft(uri) { case (u, (k, v)) => u.addParam(k, v) }

      basicRequest
        .get(uriWithParams)
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

    override def delete(
        endpoint: String
    ): F[Either[GeminiError, Unit]] = {
      val uri = uri"$baseUrl".addPath(endpoint.split('/')).addParam("key", apiKey.value)

      basicRequest
        .delete(uri)
        .mapResponse(_ => Right(())) // Map success to Unit
        .send(backend)
        .map { response =>
          if (response.code.isSuccess) Right(())
          else Left(GeminiError.InvalidRequest(s"API error: ${response.code}", None))
        }
        .handleError(error => Left(GeminiError.ConnectionError(error.getMessage, Some(error))))
    }

    override def startResumableUpload(
        uri: String,
        metadata: String,
        headers: Map[String, String]
    ): F[Either[GeminiError, String]] = {
      val requestUri = uri"$uri".addParam("key", apiKey.value)

      basicRequest
        .post(requestUri)
        .headers(headers)
        .body(metadata)
        .contentType("application/json")
        .mapResponse { _ =>
          // We only care about headers here, specifically X-Goog-Upload-URL
          // sttp response body is mapped, but we need access to headers in the response object
          ""
        }
        .send(backend)
        .map { response =>
          if (response.code.isSuccess) {
            response.header("X-Goog-Upload-URL") match {
              case Some(uploadUrl) => Right(uploadUrl)
              case None            => Left(GeminiError.InvalidRequest("Missing X-Goog-Upload-URL header", None))
            }
          } else {
            Left(GeminiError.InvalidRequest(s"Failed to start upload: ${response.code}", None))
          }
        }
        .handleError(error => Left(GeminiError.ConnectionError(error.getMessage, Some(error))))
    }

    override def uploadChunk(
        uploadUri: String,
        file: java.nio.file.Path,
        headers: Map[String, String]
    ): F[Either[GeminiError, gemini4s.model.domain.File]] = {
      val uri = uri"$uploadUri"

      basicRequest
        .put(uri)
        .headers(headers)
        .body(file)
        .response(asString)
        .send(backend)
        .map { response =>
          response.body match {
            case Right(body) => io.circe.parser.parse(body).flatMap { json =>
                // The API returns { "file": { ... } } for upload response
                val fileJson = json.hcursor.downField("file").focus.getOrElse(json)
                fileJson.as[gemini4s.model.domain.File]
              } match {
                case Right(file) => Right(file)
                case Left(error) => Left(
                    GeminiError
                      .InvalidRequest(s"Upload failed: ${response.code} - ${error.getMessage}. Raw body: $body", None)
                  )
              }
            case Left(error) => Left(GeminiError.InvalidRequest(s"Upload failed: ${response.code} - $error", None))
          }
        }
        .handleError(error => Left(GeminiError.ConnectionError(error.getMessage, Some(error))))
    }

  }

}
