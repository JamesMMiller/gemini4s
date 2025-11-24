package gemini4s.interpreter

import cats.effect.IO
import fs2.Stream
import io.circe.{ Decoder, Encoder }
import munit.CatsEffectSuite

import gemini4s.GeminiService
import gemini4s.error.GeminiError
import gemini4s.http.GeminiHttpClient
import gemini4s.model.domain._
import gemini4s.model.request._
import gemini4s.model.response._

class GeminiServiceImplSpec extends CatsEffectSuite {

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

  }

  test("generateContent should call client with correct request and endpoint") {
    val expectedResponse = GenerateContentResponse(List.empty, None, None)
    val client           = new MockHttpClient(response = Right(expectedResponse))
    val service          = GeminiServiceImpl.make[IO](client)
    val contents         = List(Content(List(ContentPart("test"))))
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
    val service          = GeminiServiceImpl.make[IO](client)
    val contents         = List(Content(List(ContentPart("test"))))
    val config           = GenerationConfig(temperature = Some(0.5f))
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
    val service          = GeminiServiceImpl.make[IO](client)
    val contents         = List(Content(List(ContentPart("test"))))
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
    val service          = GeminiServiceImpl.make[IO](client)
    val content          = Content(List(ContentPart("test")))
    val request          = EmbedContentRequest(content, GeminiConstants.EmbeddingText004)

    service.embedContent(request).map { result =>
      assertEquals(result, Right(ContentEmbedding(List(0.1f))))
      assertEquals(
        client.lastEndpoint,
        GeminiConstants.Endpoints.embedContent(GeminiConstants.EmbeddingText004)
      )
      assert(client.lastRequest.exists(_.isInstanceOf[EmbedContentRequest]))
    }
  }

  test("batchEmbedContents should call client with correct request") {
    val expectedResponse = BatchEmbedContentsResponse(List(ContentEmbedding(List(0.1f))))
    val client           = new MockHttpClient(response = Right(expectedResponse))
    val service          = GeminiServiceImpl.make[IO](client)
    val model            = GeminiConstants.EmbeddingText004
    val requests         = List(EmbedContentRequest(Content(List(ContentPart("test"))), model))
    val batchRequest     = BatchEmbedContentsRequest(model, requests)

    service.batchEmbedContents(batchRequest).map { result =>
      assertEquals(result, Right(List(ContentEmbedding(List(0.1f)))))
      assertEquals(
        client.lastEndpoint,
        GeminiConstants.Endpoints.batchEmbedContents(GeminiConstants.EmbeddingText004)
      )
      assert(client.lastRequest.exists(_.isInstanceOf[BatchEmbedContentsRequest]))
    }
  }

  test("createCachedContent should call client with correct request") {
    val expectedResponse = CachedContent("name", "model", "now", "now", "later")
    val client           = new MockHttpClient(response = Right(expectedResponse))
    val service          = GeminiServiceImpl.make[IO](client)
    val request          = CreateCachedContentRequest(
      model = Some("model"),
      contents = Some(List(Content(List(ContentPart("test")))))
    )

    service.createCachedContent(request).map { result =>
      assertEquals(result, Right(expectedResponse))
      assertEquals(client.lastEndpoint, GeminiConstants.Endpoints.createCachedContent)
      assert(client.lastRequest.exists(_.isInstanceOf[CreateCachedContentRequest]))
    }
  }
}
