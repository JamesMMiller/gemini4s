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

import gemini4s.config.ApiKey
import gemini4s.error.GeminiError
import gemini4s.model.domain._
import gemini4s.model.request._
import gemini4s.model.response._

class GeminiHttpClientSpec extends CatsEffectSuite {

  val apiKey: ApiKey = ApiKey.unsafe("test-api-key")

  // Helper to create a backend that satisfies the Fs2Streams requirement
  def createBackend(stub: SttpBackendStub[IO, Any]): SttpBackend[IO, Fs2Streams[IO]] =
    stub.asInstanceOf[SttpBackend[IO, Fs2Streams[IO]]]

  test("post should handle successful response") {
    val response = GenerateContentResponse(
      candidates = List(
        Candidate(
          content = Some(
            ResponseContent(
              parts = List(ResponsePart.Text(text = "Generated text")),
              role = Some("model")
            )
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

    val client  = GeminiHttpClient.make[IO](createBackend(ioBackend), apiKey)
    val request = GenerateContentRequest(
      ModelName.unsafe("gemini-2.0-flash-lite-preview-02-05"),
      List(Content(parts = List(ContentPart.Text("prompt"))))
    )

    client.post[GenerateContentRequest, GenerateContentResponse]("generateContent", request).map { result =>
      assert(result.isRight)
      assertEquals(
        result.map(_.candidates.head.content.flatMap(_.parts.headOption).get.asInstanceOf[ResponsePart.Text].text),
        Right("Generated text")
      )
    }
  }

  test("post should handle API errors") {
    val ioBackend = SttpBackendStub(implicitly[sttp.monad.MonadError[IO]]).whenAnyRequest
      .thenRespond("Invalid request", StatusCode.BadRequest)

    val client  = GeminiHttpClient.make[IO](createBackend(ioBackend), apiKey)
    val request = GenerateContentRequest(
      ModelName.unsafe("gemini-2.0-flash-lite-preview-02-05"),
      List(Content(parts = List(ContentPart.Text("prompt"))))
    )

    client.post[GenerateContentRequest, GenerateContentResponse]("generateContent", request).map { result =>
      assert(result.isLeft)
      assert(result.left.exists(_.isInstanceOf[GeminiError.InvalidRequest]))
    }
  }

  test("post should handle network errors") {
    val ioBackend = SttpBackendStub(implicitly[sttp.monad.MonadError[IO]]).whenAnyRequest.thenRespondServerError()

    val client  = GeminiHttpClient.make[IO](createBackend(ioBackend), apiKey)
    val request = GenerateContentRequest(
      ModelName.unsafe("gemini-2.0-flash-lite-preview-02-05"),
      List(Content(parts = List(ContentPart.Text("prompt"))))
    )

    client.post[GenerateContentRequest, GenerateContentResponse]("generateContent", request).map { result =>
      assert(result.isLeft)
      assert(result.left.exists(_.isInstanceOf[GeminiError.InvalidRequest]))
    }
  }

  test("post should handle connection exceptions") {
    val ioBackend = SttpBackendStub(implicitly[sttp.monad.MonadError[IO]]).whenAnyRequest.thenRespond(
      throw new java.net.ConnectException("Connection refused")
    )

    val client  = GeminiHttpClient.make[IO](createBackend(ioBackend), apiKey)
    val request = GenerateContentRequest(
      ModelName.unsafe("gemini-2.0-flash-lite-preview-02-05"),
      List(Content(parts = List(ContentPart.Text("prompt"))))
    )

    client.post[GenerateContentRequest, GenerateContentResponse]("generateContent", request).map { result =>
      assert(result.isLeft)
      assert(result.left.exists(_.isInstanceOf[GeminiError.ConnectionError]))
    }
  }

  test("postStream should handle successful streaming response") {
    val response1 = GenerateContentResponse(
      candidates = List(
        Candidate(
          content = Some(ResponseContent(List(ResponsePart.Text("Hello")), Some("model"))),
          finishReason = Some("STOP"),
          index = None,
          safetyRatings = None
        )
      ),
      usageMetadata = None,
      modelVersion = None
    )
    val response2 = GenerateContentResponse(
      candidates = List(
        Candidate(
          content = Some(ResponseContent(List(ResponsePart.Text(" World")), Some("model"))),
          finishReason = Some("STOP"),
          index = None,
          safetyRatings = None
        )
      ),
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

    val client  = GeminiHttpClient.make[IO](createBackend(ioBackend), apiKey)
    val request = GenerateContentRequest(
      ModelName.unsafe("gemini-2.0-flash-lite-preview-02-05"),
      List(Content(parts = List(ContentPart.Text("prompt"))))
    )

    client
      .postStream[GenerateContentRequest, GenerateContentResponse]("streamGenerateContent", request)
      .compile
      .toList
      .map { results =>
        assertEquals(results.length, 2)
        assertEquals(
          results(0).candidates.head.content.flatMap(_.parts.headOption).get.asInstanceOf[ResponsePart.Text].text,
          "Hello"
        )
        assertEquals(
          results(1).candidates.head.content.flatMap(_.parts.headOption).get.asInstanceOf[ResponsePart.Text].text,
          " World"
        )
      }
  }

  test("postStream should handle stream errors") {
    val ioBackend = SttpBackendStub(implicitly[sttp.monad.MonadError[IO]]).whenAnyRequest
      .thenRespond(Left("Invalid request"), StatusCode.BadRequest)

    val client  = GeminiHttpClient.make[IO](createBackend(ioBackend), apiKey)
    val request = GenerateContentRequest(
      ModelName.unsafe("gemini-2.0-flash-lite-preview-02-05"),
      List(Content(parts = List(ContentPart.Text("prompt"))))
    )

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

  test("postStream should handle connection exceptions") {
    val ioBackend = SttpBackendStub(implicitly[sttp.monad.MonadError[IO]]).whenAnyRequest.thenRespond(
      throw new java.net.ConnectException("Connection refused")
    )

    val client  = GeminiHttpClient.make[IO](createBackend(ioBackend), apiKey)
    val request = GenerateContentRequest(
      ModelName.unsafe("gemini-2.0-flash-lite-preview-02-05"),
      List(Content(parts = List(ContentPart.Text("prompt"))))
    )

    client
      .postStream[GenerateContentRequest, GenerateContentResponse]("streamGenerateContent", request)
      .compile
      .drain
      .attempt
      .map { result =>
        assert(result.isLeft)
        assert(result.left.exists(_.isInstanceOf[GeminiError.ConnectionError]))
      }
  }
  test("startResumableUpload should return upload URL") {
    val uploadUrl = "http://upload-url"
    val ioBackend = SttpBackendStub(implicitly[sttp.monad.MonadError[IO]])
      .whenRequestMatches(_.uri.path.mkString("/").contains("upload/v1beta/files"))
      .thenRespond(
        sttp.client3.Response("", StatusCode.Ok, "", List(sttp.model.Header("X-Goog-Upload-URL", uploadUrl)))
      )

    val client = GeminiHttpClient.make[IO](createBackend(ioBackend), apiKey)

    client.startResumableUpload("http://base-url/upload/v1beta/files", "{}", Map.empty).map { result =>
      assertEquals(result, Right(uploadUrl))
    }
  }

  test("uploadChunk should return File") {
    val file         = File(
      name = "files/123",
      uri = FileUri("http://file-uri")
    )
    // The API returns { "file": { ... } }
    val responseBody = io.circe.Json.obj("file" -> file.asJson).noSpaces

    val ioBackend = SttpBackendStub(implicitly[sttp.monad.MonadError[IO]])
      .whenRequestMatches(_.uri.toString == "http://upload-url")
      .thenRespond(responseBody)

    val client = GeminiHttpClient.make[IO](createBackend(ioBackend), apiKey)
    val path   = java.nio.file.Paths.get("test.txt")

    // We need a real file for the body, so let's create a temp one
    val tempFile = java.nio.file.Files.createTempFile("test", ".txt")
    java.nio.file.Files.write(tempFile, "content".getBytes)

    client.uploadChunk("http://upload-url", tempFile, Map.empty).map(result => assertEquals(result, Right(file)))
  }

  test("delete should return Unit") {
    val ioBackend = SttpBackendStub(implicitly[sttp.monad.MonadError[IO]])
      .whenRequestMatches(_.method == sttp.model.Method.DELETE)
      .thenRespond(StatusCode.Ok)

    val client = GeminiHttpClient.make[IO](createBackend(ioBackend), apiKey)

    client.delete("files/123").map(result => assertEquals(result, Right(())))
  }

  test("startResumableUpload should handle errors") {
    val ioBackend =
      SttpBackendStub(implicitly[sttp.monad.MonadError[IO]]).whenAnyRequest.thenRespond(StatusCode.BadRequest)

    val client = GeminiHttpClient.make[IO](createBackend(ioBackend), apiKey)

    client.startResumableUpload("http://base-url", "{}", Map.empty).map(result => assert(result.isLeft))
  }

  test("uploadChunk should handle errors") {
    val ioBackend =
      SttpBackendStub(implicitly[sttp.monad.MonadError[IO]]).whenAnyRequest.thenRespond(StatusCode.BadRequest)

    val client   = GeminiHttpClient.make[IO](createBackend(ioBackend), apiKey)
    val tempFile = java.nio.file.Files.createTempFile("test", ".txt")

    client.uploadChunk("http://upload-url", tempFile, Map.empty).map(result => assert(result.isLeft))
  }

  test("delete should handle errors") {
    val ioBackend =
      SttpBackendStub(implicitly[sttp.monad.MonadError[IO]]).whenAnyRequest.thenRespond("error", StatusCode.BadRequest)

    val client = GeminiHttpClient.make[IO](createBackend(ioBackend), apiKey)

    client.delete("files/123").map(result => assert(result.isLeft))
  }
}
