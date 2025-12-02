package gemini4s

import cats.effect.IO
import fs2.Stream
import io.circe.{ Decoder, Encoder }
import munit.CatsEffectSuite

import gemini4s.config.{ ApiKey, GeminiConfig }
import gemini4s.error.GeminiError
import gemini4s.http.GeminiHttpClient
import gemini4s.model.domain._
import gemini4s.model.request._
import gemini4s.model.response._

class GeminiServiceSpec extends CatsEffectSuite {

  class MockHttpClient(
      response: Either[GeminiError, Any] = Right(()),
      streamResponse: Stream[IO, Any] = Stream.empty
  ) extends GeminiHttpClient[IO] {
    var lastEndpoint: String     = ""
    var lastRequest: Option[Any] = None

    override def post[Req: Encoder, Res: Decoder](
        endpoint: String,
        request: Req
    ): IO[Either[GeminiError, Res]] = {
      lastEndpoint = endpoint
      lastRequest = Some(request)
      IO.pure(response.asInstanceOf[Either[GeminiError, Res]])
    }

    override def postStream[Req: Encoder, Res: Decoder](
        endpoint: String,
        request: Req
    ): Stream[IO, Res] = {
      lastEndpoint = endpoint
      lastRequest = Some(request)
      streamResponse.asInstanceOf[Stream[IO, Res]]
    }

    override def get[Res: Decoder](
        endpoint: String,
        params: Map[String, String]
    ): IO[Either[GeminiError, Res]] = IO.pure(Left(GeminiError.InvalidRequest("Not implemented", None)))

    override def delete(
        endpoint: String
    ): IO[Either[GeminiError, Unit]] = IO.pure(Right(()))

    override def startResumableUpload(
        uri: String,
        metadata: String,
        headers: Map[String, String]
    ): IO[Either[GeminiError, String]] = IO.pure(Right("http://upload-url"))

    override def uploadChunk(
        uploadUri: String,
        file: java.nio.file.Path,
        headers: Map[String, String]
    ): IO[Either[GeminiError, gemini4s.model.domain.File]] = IO.pure(
      Right(
        gemini4s.model.domain.File(
          name = "files/123",
          displayName = Some("test.txt"),
          mimeType = Some(gemini4s.model.domain.MimeType.unsafe("text/plain")),
          sizeBytes = Some(gemini4s.model.domain.SizeBytes(100L)),
          createTime = Some(java.time.Instant.now().toString),
          updateTime = Some(java.time.Instant.now().toString),
          expirationTime = Some(java.time.Instant.now().toString),
          sha256Hash = Some("hash"),
          uri = gemini4s.model.domain.FileUri("http://file-uri"),
          state = Some(gemini4s.model.domain.FileState.ACTIVE),
          error = None
        )
      )
    )

  }

  test("generateContent should call client with correct request and endpoint") {
    val expectedResponse = GenerateContentResponse(List.empty, None, None)
    val client           = new MockHttpClient(response = Right(expectedResponse))
    val service          = GeminiService.make[IO](client)
    val contents         = List(Content(List(ContentPart.Text("test"))))
    val request          = GenerateContentRequest(GeminiConstants.DefaultModel, contents)

    service.generateContent(request).map { result =>
      assertEquals(result, Right(expectedResponse))
      assertEquals(client.lastEndpoint, GeminiConstants.Endpoints.generateContent())
      assert(client.lastRequest.exists(_.isInstanceOf[GenerateContentRequest]))
      assertEquals(
        client.lastRequest.map(_.asInstanceOf[GenerateContentRequest].contents),
        Some(contents)
      )
    }
  }

  test("generateContentStream should call client with correct request and endpoint") {
    val expectedResponse = GenerateContentResponse(List.empty, None, None)
    val client           = new MockHttpClient(streamResponse = Stream.emit(expectedResponse))
    val service          = GeminiService.make[IO](client)
    val contents         = List(Content(List(ContentPart.Text("test"))))
    val config           = GenerationConfig(temperature = Some(Temperature.unsafe(0.5f)))
    val request          = GenerateContentRequest(GeminiConstants.DefaultModel, contents, generationConfig = Some(config))

    service.generateContentStream(request).compile.toList.map { results =>
      assertEquals(results, List(expectedResponse))
      assertEquals(client.lastEndpoint, GeminiConstants.Endpoints.generateContentStream())
      assert(client.lastRequest.exists(_.isInstanceOf[GenerateContentRequest]))
      val req = client.lastRequest.get.asInstanceOf[GenerateContentRequest]
      assertEquals(req.generationConfig, Some(config))
    }
  }

  test("countTokens should call client with correct request") {
    val expectedResponse = CountTokensResponse(100)
    val client           = new MockHttpClient(response = Right(expectedResponse))
    val service          = GeminiService.make[IO](client)
    val contents         = List(Content(List(ContentPart.Text("test"))))
    val request          = CountTokensRequest(GeminiConstants.DefaultModel, contents)

    service.countTokens(request).map { result =>
      assertEquals(result, Right(100))
      assertEquals(client.lastEndpoint, GeminiConstants.Endpoints.countTokens())
      assert(client.lastRequest.exists(_.isInstanceOf[CountTokensRequest]))
    }
  }

  test("embedContent should call client with correct request") {
    val expectedResponse = EmbedContentResponse(ContentEmbedding(List(0.1f)))
    val client           = new MockHttpClient(response = Right(expectedResponse))
    val service          = GeminiService.make[IO](client)
    val content          = Content(List(ContentPart.Text("test")))
    val request          = EmbedContentRequest(content, GeminiConstants.EmbeddingGemini001)

    service.embedContent(request).map { result =>
      assertEquals(result, Right(ContentEmbedding(List(0.1f))))
      assertEquals(
        client.lastEndpoint,
        GeminiConstants.Endpoints.embedContent(GeminiConstants.EmbeddingGemini001)
      )
      assert(client.lastRequest.exists(_.isInstanceOf[EmbedContentRequest]))
    }
  }

  test("batchEmbedContents should call client with correct request") {
    val expectedResponse = BatchEmbedContentsResponse(List(ContentEmbedding(List(0.1f))))
    val client           = new MockHttpClient(response = Right(expectedResponse))
    val service          = GeminiService.make[IO](client)
    val model            = GeminiConstants.EmbeddingGemini001
    val requests         = List(EmbedContentRequest(Content(List(ContentPart.Text("test"))), model))
    val batchRequest     = BatchEmbedContentsRequest(model, requests)

    service.batchEmbedContents(batchRequest).map { result =>
      assertEquals(result, Right(List(ContentEmbedding(List(0.1f)))))
      assertEquals(
        client.lastEndpoint,
        GeminiConstants.Endpoints.batchEmbedContents(GeminiConstants.EmbeddingGemini001)
      )
      assert(client.lastRequest.exists(_.isInstanceOf[BatchEmbedContentsRequest]))
    }
  }

  test("createCachedContent should call client with correct request") {
    val expectedResponse = CachedContent("name", "model", "now", "now", "later")
    val client           = new MockHttpClient(response = Right(expectedResponse))
    val service          = GeminiService.make[IO](client)
    val request          = CreateCachedContentRequest(
      model = Some("model"),
      contents = Some(List(Content(List(ContentPart.Text("test")))))
    )

    service.createCachedContent(request).map { result =>
      assertEquals(result, Right(expectedResponse))
      assertEquals(client.lastEndpoint, GeminiConstants.Endpoints.createCachedContent)
      assert(client.lastRequest.exists(_.isInstanceOf[CreateCachedContentRequest]))
    }
  }
  test("make with config should create service") {
    val config   = GeminiConfig("test-key")
    val resource = GeminiService.make[IO](config)
    resource.use(service => IO(assert(service != null)))
  }
  test("countTokens should handle errors") {
    val error   = GeminiError.InvalidRequest("error")
    val client  = new MockHttpClient(response = Left(error))
    val service = GeminiService.make[IO](client)
    val request = CountTokensRequest(GeminiConstants.DefaultModel, List(Content(List(ContentPart.Text("test")))))

    service.countTokens(request).map(result => assertEquals(result, Left(error)))
  }

  test("embedContent should handle errors") {
    val error   = GeminiError.InvalidRequest("error")
    val client  = new MockHttpClient(response = Left(error))
    val service = GeminiService.make[IO](client)
    val request = EmbedContentRequest(Content(List(ContentPart.Text("test"))), GeminiConstants.EmbeddingGemini001)

    service.embedContent(request).map(result => assertEquals(result, Left(error)))
  }

  test("batchEmbedContents should handle errors") {
    val error   = GeminiError.InvalidRequest("error")
    val client  = new MockHttpClient(response = Left(error))
    val service = GeminiService.make[IO](client)
    val request = BatchEmbedContentsRequest(GeminiConstants.EmbeddingGemini001, List.empty)

    service.batchEmbedContents(request).map(result => assertEquals(result, Left(error)))
  }

  test("createCachedContent should handle errors") {
    val error   = GeminiError.InvalidRequest("error")
    val client  = new MockHttpClient(response = Left(error))
    val service = GeminiService.make[IO](client)
    val request = CreateCachedContentRequest(model = Some("model"))

    service.createCachedContent(request).map(result => assertEquals(result, Left(error)))
  }
}
