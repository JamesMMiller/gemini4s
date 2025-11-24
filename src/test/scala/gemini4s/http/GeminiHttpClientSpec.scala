package gemini4s.http

import cats.effect.IO
import fs2.Stream
import io.circe.syntax._
import munit.CatsEffectSuite
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend
import sttp.client3.impl.cats.implicits._
import sttp.client3.testing.SttpBackendStub
import sttp.model.StatusCode

import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError
import gemini4s.model.domain._
import gemini4s.model.request._
import gemini4s.model.response._

class GeminiHttpClientSpec extends CatsEffectSuite {

  given config: GeminiConfig = GeminiConfig("test-api-key")

  // Helper to create a backend that satisfies the Fs2Streams requirement
  def createBackend(stub: SttpBackendStub[IO, Any]): SttpBackend[IO, Fs2Streams[IO]] =
    stub.asInstanceOf[SttpBackend[IO, Fs2Streams[IO]]]

  test("post should handle successful response") {
    val response = GenerateContentResponse(
      candidates = List(
        Candidate(
          content = ResponseContent(
            parts = List(ResponsePart.Text(text = "Generated text")),
            role = Some("model")
          ),
          finishReason = Some("STOP"),
          index = None,
          safetyRatings = None
        )
      ),
      usageMetadata = None,
      modelVersion = None
    )

    val ioBackend = SttpBackendStub(implicitly[sttp.monad.MonadError[IO]])
      .whenRequestMatches(_.uri.path.mkString("/").contains("generateContent"))
      .thenRespond(response.asJson.noSpaces)

    val client  = GeminiHttpClient.make[IO](createBackend(ioBackend), config)
    val request =
      GenerateContentRequest("gemini-2.0-flash-lite-preview-02-05", List(Content(parts = List(ContentPart("prompt")))))

    client.post[GenerateContentRequest, GenerateContentResponse]("generateContent", request).map { result =>
      assert(result.isRight)
      assertEquals(
        result.map(_.candidates.head.content.parts.head.asInstanceOf[ResponsePart.Text].text),
        Right("Generated text")
      )
    }
  }

  test("post should handle API errors") {
    val ioBackend = SttpBackendStub(implicitly[sttp.monad.MonadError[IO]]).whenAnyRequest
      .thenRespond("Invalid request", StatusCode.BadRequest)

    val client  = GeminiHttpClient.make[IO](createBackend(ioBackend), config)
    val request =
      GenerateContentRequest("gemini-2.0-flash-lite-preview-02-05", List(Content(parts = List(ContentPart("prompt")))))

    client.post[GenerateContentRequest, GenerateContentResponse]("generateContent", request).map { result =>
      assert(result.isLeft)
      assert(result.left.exists(_.isInstanceOf[GeminiError.InvalidRequest]))
    }
  }

  test("post should handle network errors") {
    val ioBackend = SttpBackendStub(implicitly[sttp.monad.MonadError[IO]]).whenAnyRequest.thenRespondServerError()

    val client  = GeminiHttpClient.make[IO](createBackend(ioBackend), config)
    val request =
      GenerateContentRequest("gemini-2.0-flash-lite-preview-02-05", List(Content(parts = List(ContentPart("prompt")))))

    client.post[GenerateContentRequest, GenerateContentResponse]("generateContent", request).map { result =>
      assert(result.isLeft)
      assert(result.left.exists(_.isInstanceOf[GeminiError.InvalidRequest]))
    }
  }

  test("post should handle connection exceptions") {
    val ioBackend = SttpBackendStub(implicitly[sttp.monad.MonadError[IO]]).whenAnyRequest.thenRespond(
      throw new java.net.ConnectException("Connection refused")
    )

    val client  = GeminiHttpClient.make[IO](createBackend(ioBackend), config)
    val request =
      GenerateContentRequest("gemini-2.0-flash-lite-preview-02-05", List(Content(parts = List(ContentPart("prompt")))))

    client.post[GenerateContentRequest, GenerateContentResponse]("generateContent", request).map { result =>
      assert(result.isLeft)
      assert(result.left.exists(_.isInstanceOf[GeminiError.ConnectionError]))
    }
  }

  test("postStream should handle successful streaming response") {
    val response1 = GenerateContentResponse(
      candidates =
        List(Candidate(ResponseContent(List(ResponsePart.Text("Hello")), Some("model")), Some("STOP"), None, None)),
      usageMetadata = None,
      modelVersion = None
    )
    val response2 = GenerateContentResponse(
      candidates =
        List(Candidate(ResponseContent(List(ResponsePart.Text(" World")), Some("model")), Some("STOP"), None, None)),
      usageMetadata = None,
      modelVersion = None
    )

    val jsonArrayStream = Stream.emit("[").covary[IO] ++
      Stream.emit(response1.asJson.noSpaces) ++
      Stream.emit(",") ++
      Stream.emit(response2.asJson.noSpaces) ++
      Stream.emit("]")

    val byteStream = jsonArrayStream.through(fs2.text.utf8.encode)

    val ioBackend = SttpBackendStub(implicitly[sttp.monad.MonadError[IO]])
      .whenRequestMatches(_.uri.path.mkString("/").contains("streamGenerateContent"))
      .thenRespond(Right(byteStream))

    val client  = GeminiHttpClient.make[IO](createBackend(ioBackend), config)
    val request =
      GenerateContentRequest("gemini-2.0-flash-lite-preview-02-05", List(Content(parts = List(ContentPart("prompt")))))

    client
      .postStream[GenerateContentRequest, GenerateContentResponse]("streamGenerateContent", request)
      .compile
      .toList
      .map { results =>
        assertEquals(results.length, 2)
        assertEquals(results(0).candidates.head.content.parts.head.asInstanceOf[ResponsePart.Text].text, "Hello")
        assertEquals(results(1).candidates.head.content.parts.head.asInstanceOf[ResponsePart.Text].text, " World")
      }
  }

  test("postStream should handle stream errors") {
    val ioBackend = SttpBackendStub(implicitly[sttp.monad.MonadError[IO]]).whenAnyRequest
      .thenRespond(Left("Invalid request"), StatusCode.BadRequest)

    val client  = GeminiHttpClient.make[IO](createBackend(ioBackend), config)
    val request =
      GenerateContentRequest("gemini-2.0-flash-lite-preview-02-05", List(Content(parts = List(ContentPart("prompt")))))

    client
      .postStream[GenerateContentRequest, GenerateContentResponse]("streamGenerateContent", request)
      .compile
      .drain
      .attempt
      .map { result =>
        assert(result.isLeft)
        assert(result.left.exists(_.isInstanceOf[GeminiError.InvalidRequest]))
      }
  }
}
