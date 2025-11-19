package gemini4s.interpreter

import cats.effect.IO
import fs2.Stream
import io.circe.{Decoder, Encoder}
import munit.CatsEffectSuite

import gemini4s.GeminiService
import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError
import gemini4s.http.GeminiHttpClient
import gemini4s.model.GeminiRequest
import gemini4s.model.GeminiRequest._
import gemini4s.model.GeminiResponse._

class GeminiServiceImplSpec extends CatsEffectSuite {

  given config: GeminiConfig = GeminiConfig("test-key")

  class MockHttpClient(
      response: Either[GeminiError, Any] = Right(()),
      streamResponse: Stream[IO, Any] = Stream.empty
  ) extends GeminiHttpClient[IO] {
    var lastEndpoint: String               = ""
    var lastRequest: Option[GeminiRequest] = None

    override def post[Req <: GeminiRequest: Encoder, Res: Decoder](
        endpoint: String,
        request: Req
    )(using config: GeminiConfig): IO[Either[GeminiError, Res]] = {
      lastEndpoint = endpoint
      lastRequest = Some(request)
      IO.pure(response.asInstanceOf[Either[GeminiError, Res]])
    }

    override def postStream[Req <: GeminiRequest: Encoder, Res: Decoder](
        endpoint: String,
        request: Req
    )(using config: GeminiConfig): Stream[IO, Res] = {
      lastEndpoint = endpoint
      lastRequest = Some(request)
      streamResponse.asInstanceOf[Stream[IO, Res]]
    }
  }

  test("generateContent should call client with correct request and endpoint") {
    val expectedResponse = GenerateContentResponse(List.empty, None, None)
    val client           = new MockHttpClient(response = Right(expectedResponse))
    val service          = GeminiServiceImpl.make[IO](client)
    val contents         = List(Content(List(Part("test"))))

    service.generateContent(contents, None, None, None, None, None).map { result =>
      assertEquals(result, Right(expectedResponse))
      assertEquals(client.lastEndpoint, GeminiService.Endpoints.generateContent())
      assert(client.lastRequest.exists(_.isInstanceOf[GenerateContent]))
      assertEquals(
        client.lastRequest.map(_.asInstanceOf[GenerateContent].contents),
        Some(contents)
      )
      }
  }

  test("generateContentStream should call client with correct request and endpoint") {
    val expectedResponse = GenerateContentResponse(List.empty, None, None)
    val client           = new MockHttpClient(streamResponse = Stream.emit(expectedResponse))
    val service          = GeminiServiceImpl.make[IO](client)
    val contents         = List(Content(List(Part("test"))))
    val config           = GenerationConfig(temperature = Some(0.5f))

    service
      .generateContentStream(contents, None, Some(config), None, None, None)
      .compile
      .toList
      .map { results =>
        assertEquals(results, List(expectedResponse))
        assertEquals(client.lastEndpoint, GeminiService.Endpoints.generateContentStream())
        assert(client.lastRequest.exists(_.isInstanceOf[GenerateContent]))
        val req = client.lastRequest.get.asInstanceOf[GenerateContent]
        assertEquals(req.generationConfig, Some(config))
      }
  }

  test("generateContentStream should use default arguments") {
    val expectedResponse = GenerateContentResponse(List.empty, None, None)
    val client           = new MockHttpClient(streamResponse = Stream.emit(expectedResponse))
    val service          = GeminiServiceImpl.make[IO](client)
    val contents         = List(Content(List(Part("test"))))

    service
      .generateContentStream(contents)
      .compile
      .toList
      .map { results =>
        assertEquals(results, List(expectedResponse))
        val req = client.lastRequest.get.asInstanceOf[GenerateContent]
        assertEquals(req.generationConfig, Some(GeminiService.DefaultGenerationConfig))
        assertEquals(req.safetySettings, None)
      }
  }

  test("countTokens should call client with correct request") {
    val expectedResponse = CountTokensResponse(100)
    val client           = new MockHttpClient(response = Right(expectedResponse))
    val service          = GeminiServiceImpl.make[IO](client)
    val contents         = List(Content(List(Part("test"))))

    service.countTokens(contents).map { result =>
      assertEquals(result, Right(100))
      assertEquals(client.lastEndpoint, GeminiService.Endpoints.countTokens())
      assert(client.lastRequest.exists(_.isInstanceOf[CountTokensRequest]))
    }
  }

  test("embedContent should call client with correct request") {
    val expectedResponse = EmbedContentResponse(ContentEmbedding(List(0.1f)))
    val client           = new MockHttpClient(response = Right(expectedResponse))
    val service          = GeminiServiceImpl.make[IO](client)
    val content          = Content(List(Part("test")))

    service.embedContent(content).map { result =>
      assertEquals(result, Right(ContentEmbedding(List(0.1f))))
      assertEquals(
        client.lastEndpoint,
        GeminiService.Endpoints.embedContent(GeminiService.EmbeddingText004)
      )
      assert(client.lastRequest.exists(_.isInstanceOf[EmbedContentRequest]))
    }
  }

  test("batchEmbedContents should call client with correct request") {
    val expectedResponse = BatchEmbedContentsResponse(List(ContentEmbedding(List(0.1f))))
    val client           = new MockHttpClient(response = Right(expectedResponse))
    val service          = GeminiServiceImpl.make[IO](client)
    val requests         = List(EmbedContentRequest(Content(List(Part("test"))), "model"))

    service.batchEmbedContents(requests).map { result =>
      assertEquals(result, Right(List(ContentEmbedding(List(0.1f)))))
      assertEquals(
        client.lastEndpoint,
        GeminiService.Endpoints.batchEmbedContents(GeminiService.EmbeddingText004)
      )
      assert(client.lastRequest.exists(_.isInstanceOf[BatchEmbedContentsRequest]))
    }
  }

  test("createCachedContent should call client with correct request") {
    val expectedResponse = CachedContent("name", "model", "now", "now", "later")
    val client           = new MockHttpClient(response = Right(expectedResponse))
    val service          = GeminiServiceImpl.make[IO](client)

    service
      .createCachedContent(
        model = "model",
        contents = Some(List(Content(List(Part("test")))))
      )
      .map { result =>
        assertEquals(result, Right(expectedResponse))
        assertEquals(client.lastEndpoint, GeminiService.Endpoints.createCachedContent)
        assert(client.lastRequest.exists(_.isInstanceOf[CreateCachedContentRequest]))
      }
  }
}
