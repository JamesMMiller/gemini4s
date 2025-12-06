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
    ): IO[Either[GeminiError, Res]] = {
      lastEndpoint = endpoint
      IO.pure(response.asInstanceOf[Either[GeminiError, Res]])
    }

    override def delete(
        endpoint: String
    ): IO[Either[GeminiError, Unit]] = {
      lastEndpoint = endpoint
      IO.pure(Right(()))
    }

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

  test("batchGenerateContent should call client with correct request") {
    val expectedResponse = BatchJob("job-name", BatchJobState.JOB_STATE_PENDING, "now", "now")
    val client           = new MockHttpClient(response = Right(expectedResponse))
    val service          = GeminiService.make[IO](client)
    val model            = GeminiConstants.DefaultModel
    val requests         = List(GenerateContentRequest(model, List(Content(List(ContentPart.Text("test"))))))

    service.batchGenerateContent(model, requests).map { result =>
      assertEquals(result, Right(expectedResponse))
      assertEquals(client.lastEndpoint, GeminiConstants.Endpoints.batchGenerateContent(model))
      assert(client.lastRequest.exists(_.isInstanceOf[BatchGenerateContentRequest]))
    }
  }

  test("getBatchJob should call client with correct request") {
    val expectedResponse = BatchJob("job-name", BatchJobState.JOB_STATE_SUCCEEDED, "now", "now")
    val client           = new MockHttpClient(response = Right(expectedResponse))
    val service          = GeminiService.make[IO](client)

    service.getBatchJob("job-name").map { result =>
      assertEquals(result, Right(expectedResponse))
      assertEquals(client.lastEndpoint, "job-name")
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

  test("batchGenerateContent should handle errors") {
    val error    = GeminiError.InvalidRequest("error")
    val client   = new MockHttpClient(response = Left(error))
    val service  = GeminiService.make[IO](client)
    val model    = GeminiConstants.DefaultModel
    val requests = List(GenerateContentRequest(model, List(Content(List(ContentPart.Text("test"))))))

    service.batchGenerateContent(model, requests).map(result => assertEquals(result, Left(error)))
  }

  test("createCachedContent should handle errors") {
    val error   = GeminiError.InvalidRequest("error")
    val client  = new MockHttpClient(response = Left(error))
    val service = GeminiService.make[IO](client)
    val request = CreateCachedContentRequest(model = Some("model"))

    service.createCachedContent(request).map(result => assertEquals(result, Left(error)))
  }
  test("uploadFile should call client with correct request") {
    val expectedFile = File(
      name = "files/123",
      displayName = Some("test.txt"),
      mimeType = Some(MimeType.unsafe("text/plain")),
      sizeBytes = Some(SizeBytes(100L)),
      createTime = Some("now"),
      updateTime = Some("now"),
      expirationTime = Some("later"),
      sha256Hash = Some("hash"),
      uri = FileUri("http://file-uri"),
      state = Some(FileState.ACTIVE),
      error = None
    )

    val client = new MockHttpClient() {
      override def startResumableUpload(
          uri: String,
          metadata: String,
          headers: Map[String, String]
      ): IO[Either[GeminiError, String]] = IO.pure(Right("http://upload-url"))

      override def uploadChunk(
          uploadUri: String,
          file: java.nio.file.Path,
          headers: Map[String, String]
      ): IO[Either[GeminiError, File]] = IO.pure(Right(expectedFile))
    }

    val service = GeminiService.make[IO](client)
    val path    = java.nio.file.Files.createTempFile("test", ".txt")
    java.nio.file.Files.write(path, "content".getBytes)

    service.uploadFile(path, "text/plain", Some("test.txt")).map(result => assertEquals(result, Right(expectedFile)))
  }

  test("listFiles should call client with correct request") {
    val expectedResponse = ListFilesResponse(Some(List.empty), None)
    val client           = new MockHttpClient() {
      override def get[Res: Decoder](
          endpoint: String,
          params: Map[String, String]
      ): IO[Either[GeminiError, Res]] = {
        lastEndpoint = endpoint
        IO.pure(Right(expectedResponse.asInstanceOf[Res]))
      }
    }
    val service          = GeminiService.make[IO](client)

    service.listFiles().map { result =>
      assertEquals(result, Right(expectedResponse))
      assertEquals(client.lastEndpoint, GeminiConstants.Endpoints.files)
    }
  }

  test("getFile should call client with correct request") {
    val expectedFile = File(
      name = "files/123",
      uri = FileUri("http://file-uri")
    )
    val client       = new MockHttpClient() {
      override def get[Res: Decoder](
          endpoint: String,
          params: Map[String, String]
      ): IO[Either[GeminiError, Res]] = {
        lastEndpoint = endpoint
        IO.pure(Right(expectedFile.asInstanceOf[Res]))
      }
    }
    val service      = GeminiService.make[IO](client)

    service.getFile("files/123").map { result =>
      assertEquals(result, Right(expectedFile))
      assertEquals(client.lastEndpoint, "files/123")
    }
  }

  test("deleteFile should call client with correct request") {
    val client  = new MockHttpClient() {
      override def delete(endpoint: String): IO[Either[GeminiError, Unit]] = {
        lastEndpoint = endpoint
        IO.pure(Right(()))
      }
    }
    val service = GeminiService.make[IO](client)

    service.deleteFile("files/123").map { result =>
      assertEquals(result, Right(()))
      assertEquals(client.lastEndpoint, "files/123")
    }
  }

  test("uploadFile should handle errors") {
    val error   = GeminiError.InvalidRequest("error")
    val client  = new MockHttpClient() {
      override def startResumableUpload(
          uri: String,
          metadata: String,
          headers: Map[String, String]
      ): IO[Either[GeminiError, String]] = IO.pure(Left(error))
    }
    val service = GeminiService.make[IO](client)
    val path    = java.nio.file.Files.createTempFile("test", ".txt")
    java.nio.file.Files.write(path, "content".getBytes)

    service.uploadFile(path, "text/plain").map(result => assertEquals(result, Left(error)))
  }

  test("listBatchJobs should call client with correct request") {
    val expectedResponse = ListBatchJobsResponse(Some(List.empty), None)
    val client           = new MockHttpClient() {
      override def get[Res: Decoder](
          endpoint: String,
          params: Map[String, String]
      ): IO[Either[GeminiError, Res]] = {
        lastEndpoint = endpoint
        IO.pure(Right(expectedResponse.asInstanceOf[Res]))
      }
    }
    val service          = GeminiService.make[IO](client)

    service.listBatchJobs().map { result =>
      assertEquals(result, Right(expectedResponse))
      assertEquals(client.lastEndpoint, GeminiConstants.Endpoints.listBatchJobs)
    }
  }

  test("cancelBatchJob should call client with correct request") {
    val client  = new MockHttpClient() {
      override def post[Req: Encoder, Res: Decoder](
          endpoint: String,
          request: Req
      ): IO[Either[GeminiError, Res]] = {
        lastEndpoint = endpoint
        IO.pure(Right(().asInstanceOf[Res]))
      }
    }
    val service = GeminiService.make[IO](client)

    service.cancelBatchJob("job-name").map { result =>
      assertEquals(result, Right(()))
      assertEquals(client.lastEndpoint, GeminiConstants.Endpoints.cancelBatchJob("job-name"))
    }
  }

  test("deleteBatchJob should call client with correct request") {
    val client  = new MockHttpClient() {
      override def delete(endpoint: String): IO[Either[GeminiError, Unit]] = {
        lastEndpoint = endpoint
        IO.pure(Right(()))
      }
    }
    val service = GeminiService.make[IO](client)

    service.deleteBatchJob("job-name").map { result =>
      assertEquals(result, Right(()))
      assertEquals(client.lastEndpoint, GeminiConstants.Endpoints.deleteBatchJob("job-name"))
    }
  }

  test("batchGenerateContent (file) should call client with correct request") {
    val expectedResponse = BatchJob("job-name", BatchJobState.JOB_STATE_PENDING, "now", "now")
    val client           = new MockHttpClient(response = Right(expectedResponse))
    val service          = GeminiService.make[IO](client)
    val model            = GeminiConstants.DefaultModel
    val dataset          = "gs://bucket/file.jsonl"

    service.batchGenerateContent(model, dataset).map { result =>
      assertEquals(result, Right(expectedResponse))
      assertEquals(client.lastEndpoint, GeminiConstants.Endpoints.batchGenerateContent(model))
      assert(client.lastRequest.exists(_.isInstanceOf[BatchGenerateContentRequest]))
      val req = client.lastRequest.get.asInstanceOf[BatchGenerateContentRequest]
      assertEquals(req.input, BatchInput.FileDataset(dataset))
    }
  }

  test("listFiles with pageSize and pageToken should pass parameters") {
    val expectedResponse                    = ListFilesResponse(Some(List.empty), None)
    var capturedParams: Map[String, String] = Map.empty
    val client                              = new MockHttpClient() {
      override def get[Res: Decoder](
          endpoint: String,
          params: Map[String, String]
      ): IO[Either[GeminiError, Res]] = {
        lastEndpoint = endpoint
        capturedParams = params
        IO.pure(Right(expectedResponse.asInstanceOf[Res]))
      }
    }
    val service                             = GeminiService.make[IO](client)

    service.listFiles(pageSize = 10, pageToken = Some("token")).map { result =>
      assertEquals(result, Right(expectedResponse))
      assertEquals(capturedParams.get("pageSize"), Some("10"))
      assertEquals(capturedParams.get("pageToken"), Some("token"))
    }
  }

  test("listBatchJobs with pageSize and pageToken should pass parameters") {
    val expectedResponse                    = ListBatchJobsResponse(Some(List.empty), None)
    var capturedParams: Map[String, String] = Map.empty
    val client                              = new MockHttpClient() {
      override def get[Res: Decoder](
          endpoint: String,
          params: Map[String, String]
      ): IO[Either[GeminiError, Res]] = {
        lastEndpoint = endpoint
        capturedParams = params
        IO.pure(Right(expectedResponse.asInstanceOf[Res]))
      }
    }
    val service                             = GeminiService.make[IO](client)

    service.listBatchJobs(pageSize = 20, pageToken = Some("batch-token")).map { result =>
      assertEquals(result, Right(expectedResponse))
      assertEquals(capturedParams.get("pageSize"), Some("20"))
      assertEquals(capturedParams.get("pageToken"), Some("batch-token"))
    }
  }
}
