package gemini4s.model

import io.circe._
import io.circe.syntax._
import munit.FunSuite

import gemini4s.model.domain._
import gemini4s.model.request._
import gemini4s.model.response._

class GeminiServiceCodecSpec extends FunSuite {

  // Requests

  test("GenerateContentRequest codec") {
    val req = GenerateContentRequest(
      model = ModelName.unsafe("gemini-2.0-flash-lite-preview-02-05"),
      contents = List(Content(List(ContentPart.Text("test")))),
      safetySettings = Some(List(SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.BLOCK_NONE))),
      generationConfig = Some(GenerationConfig(temperature = Some(Temperature.unsafe(0.5f)))),
      systemInstruction = Some(Content(List(ContentPart.Text("system")))),
      tools = Some(List(Tool(Some(List(FunctionDeclaration("name", "desc", None)))))),
      toolConfig = Some(ToolConfig(Some(FunctionCallingConfig(Some(FunctionCallingMode.AUTO)))))
    )
    assertEquals(req.asJson.as[GenerateContentRequest], Right(req))
  }

  test("CountTokensRequest codec") {
    val req = CountTokensRequest(
      ModelName.unsafe("gemini-2.0-flash-lite-preview-02-05"),
      List(Content(List(ContentPart.Text("text"))))
    )
    assertEquals(req.asJson.as[CountTokensRequest], Right(req))
  }

  test("EmbedContentRequest codec") {
    val req = EmbedContentRequest(
      Content(List(ContentPart.Text("text"))),
      ModelName.unsafe("model"),
      Some(TaskType.RETRIEVAL_QUERY),
      Some("title"),
      Some(128)
    )
    assertEquals(req.asJson.as[EmbedContentRequest], Right(req))
  }

  test("BatchEmbedContentsRequest codec") {
    val req = BatchEmbedContentsRequest(
      ModelName.unsafe("model"),
      List(EmbedContentRequest(Content(List(ContentPart.Text("text"))), ModelName.unsafe("model")))
    )
    assertEquals(req.asJson.as[BatchEmbedContentsRequest], Right(req))
  }

  test("CreateCachedContentRequest codec") {
    val req = CreateCachedContentRequest(
      model = Some("model"),
      systemInstruction = Some(Content(List(ContentPart.Text("sys")))),
      contents = Some(List(Content(List(ContentPart.Text("text"))))),
      tools = Some(List(Tool(None))),
      toolConfig = None,
      ttl = Some("3600s"),
      displayName = Some("cache")
    )
    assertEquals(req.asJson.as[CreateCachedContentRequest], Right(req))
  }

  // Responses

  test("GenerateContentResponse codec") {
    val res = GenerateContentResponse(
      candidates = List(
        Candidate(
          content = Some(ResponseContent(List(ResponsePart.Text("text")))),
          finishReason = Some("STOP"),
          index = Some(0),
          safetyRatings = Some(List(SafetyRating("category", "probability")))
        )
      ),
      usageMetadata = Some(UsageMetadata(Some(1), Some(2), 3)),
      modelVersion = Some("v1"),
      promptFeedback = Some(PromptFeedback(Some("reason"), Some(List(SafetyRating("cat", "prob")))))
    )
    assertEquals(res.asJson.as[GenerateContentResponse], Right(res))
  }

  test("CountTokensResponse codec") {
    val res = CountTokensResponse(100)
    assertEquals(res.asJson.as[CountTokensResponse], Right(res))
  }

  test("EmbedContentResponse codec") {
    val res = EmbedContentResponse(ContentEmbedding(List(0.1f, 0.2f)))
    assertEquals(res.asJson.as[EmbedContentResponse], Right(res))
  }

  test("BatchEmbedContentsResponse codec") {
    val res = BatchEmbedContentsResponse(List(ContentEmbedding(List(0.1f))))
    assertEquals(res.asJson.as[BatchEmbedContentsResponse], Right(res))
  }

  test("CachedContent codec") {
    val res = CachedContent("name", "model", "create", "update", "expire", Some("display"))
    assertEquals(res.asJson.as[CachedContent], Right(res))
  }

  // Inner types

  test("Schema codec") {
    val schema = Schema(
      `type` = SchemaType.OBJECT,
      format = Some("format"),
      description = Some("desc"),
      nullable = Some(true),
      `enum` = Some(List("a", "b")),
      properties = Some(Map("prop" -> Schema(SchemaType.STRING))),
      required = Some(List("prop"))
    )
    assertEquals(schema.asJson.as[Schema], Right(schema))
  }

  test("FunctionCallData codec") {
    val data = FunctionCallData("name", Map("arg" -> Json.fromString("value")))
    assertEquals(data.asJson.as[FunctionCallData], Right(data))
  }

  test("ExecutableCodeData codec") {
    val data = ExecutableCodeData("python", "print('hello')")
    assertEquals(data.asJson.as[ExecutableCodeData], Right(data))
  }

  test("CodeExecutionResultData codec") {
    val data = CodeExecutionResultData("ok", "output")
    assertEquals(data.asJson.as[CodeExecutionResultData], Right(data))
  }

  test("ContentPart codec") {
    val text: ContentPart = ContentPart.Text("text")
    assertEquals(text.asJson.as[ContentPart], Right(text))

    val inline: ContentPart = ContentPart.InlineData(MimeType.unsafe("image/jpeg"), ContentPart.Base64Data("base64"))
    assertEquals(inline.asJson.as[ContentPart], Right(inline))

    val file: ContentPart = ContentPart.FileData(MimeType.unsafe("application/pdf"), ContentPart.FileUri("uri"))
    assertEquals(file.asJson.as[ContentPart], Right(file))
  }

  test("SafetySetting codec") {
    val setting = SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.BLOCK_NONE)
    assertEquals(setting.asJson.as[SafetySetting], Right(setting))
  }

  test("GenerationConfig codec") {
    val config = GenerationConfig(
      temperature = Some(Temperature.unsafe(0.5f)),
      topK = Some(TopK.unsafe(10)),
      topP = Some(TopP.unsafe(0.9f)),
      candidateCount = Some(1),
      maxOutputTokens = Some(100),
      stopSequences = Some(List("stop")),
      responseMimeType = Some(MimeType.unsafe("text/plain"))
    )
    assertEquals(config.asJson.as[GenerationConfig], Right(config))
  }
}
