package gemini4s.http

import zio._
import zio.http._
import zio.json._
import zio.stream.{ZPipeline, ZStream}

import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError
import gemini4s.model.GeminiCodecs.given
import gemini4s.model.GeminiRequest

/**
 * HTTP client for communicating with the Gemini API.
 * Handles request/response serialization and error mapping.
 */
trait GeminiHttpClient[F[_]] {
  def post[Req <: GeminiRequest, Res: JsonDecoder](
    endpoint: String,
    request: Req
  )(using config: GeminiConfig): F[Either[GeminiError, Res]]

  def postStream[Req <: GeminiRequest, Res: JsonDecoder](
    endpoint: String,
    request: Req
  )(using config: GeminiConfig): F[ZStream[Any, GeminiError, Res]]
}

object GeminiHttpClient {
  val live: URLayer[Client, GeminiHttpClient[Task]] = ZLayer {
    for {
      client <- ZIO.service[Client]
    } yield new GeminiHttpClient[Task] {
      override def post[Req <: GeminiRequest, Res: JsonDecoder](
        endpoint: String,
        request: Req
      )(using config: GeminiConfig): Task[Either[GeminiError, Res]] = {
        val url = s"${config.baseUrl}/${endpoint}?key=${config.apiKey}"
        val req = Request.post(
          url = URL.decode(url).toOption.get,
          body = Body.fromString(summon[JsonEncoder[GeminiRequest]].encodeJson(request, None).toString)
        ).addHeader(Header.ContentType(MediaType.application.json))
          .addHeader(Header.Host("generativelanguage.googleapis.com"))

        ZIO.scoped {
          client.request(req).flatMap { response =>
            response.body.asString.map { body =>
              println(s"Response status: ${response.status}, body: $body") // Debug logging
              if (response.status.isSuccess) {
                body.fromJson[Res] match {
                  case Right(value) => Right(value)
                  case Left(error) => Left(GeminiError.InvalidRequest(s"Failed to decode response: $error", None))
                }
              } else {
                Left(GeminiError.InvalidRequest(s"API error: ${response.status.code} - $body", None))
              }
            }
          }.catchAll { error =>
            println(s"Connection error: ${error.getMessage}") // Debug logging
            ZIO.succeed(Left(GeminiError.ConnectionError(error.getMessage, Some(error))))
          }
        }
      }

      override def postStream[Req <: GeminiRequest, Res: JsonDecoder](
        endpoint: String,
        request: Req
      )(using config: GeminiConfig): Task[ZStream[Any, GeminiError, Res]] = {
        val url = s"${config.baseUrl}/${endpoint}?key=${config.apiKey}"
        val req = Request.post(
          url = URL.decode(url).toOption.get,
          body = Body.fromString(summon[JsonEncoder[GeminiRequest]].encodeJson(request, None).toString)
        ).addHeader(Header.ContentType(MediaType.application.json))
          .addHeader(Header.Host("generativelanguage.googleapis.com"))

        ZIO.scoped {
          client.request(req).map { response =>
            if (response.status.isSuccess) {
              response.body.asStream
                .via(ZPipeline.utf8Decode)
                .via(ZPipeline.splitLines)
                .mapZIO { line =>
                  ZIO.fromEither(line.fromJson[Res])
                    .mapError(error => GeminiError.InvalidRequest(s"Failed to decode response: $error", None))
                }
                .mapError(error => error match {
                  case e: GeminiError => e
                  case e => GeminiError.ConnectionError(e.getMessage, Some(e))
                })
            } else {
              ZStream.fail(GeminiError.InvalidRequest(s"API error: ${response.status.code}", None))
            }
          }.catchAll { error =>
            ZIO.succeed(ZStream.fail(GeminiError.ConnectionError(error.getMessage, Some(error))))
          }
        }
      }
    }
  }
} 