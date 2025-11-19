package gemini4s.interpreter

import cats.effect.Async
import cats.syntax.all._
import fs2.Stream

import gemini4s.GeminiService
import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError
import gemini4s.http.GeminiHttpClient
import gemini4s.model.GeminiRequest._
import gemini4s.model.GeminiResponse._

/**
 * Live implementation of the GeminiService using Cats Effect.
 * Uses GeminiHttpClient for API communication.
 */
final class GeminiServiceImpl[F[_]: Async](
    httpClient: GeminiHttpClient[F]
) extends GeminiService[F] {

  override def generateContent(
      contents: List[Content],
      safetySettings: Option[List[SafetySetting]],
      generationConfig: Option[GenerationConfig],
      systemInstruction: Option[Content],
      tools: Option[List[Tool]],
      toolConfig: Option[ToolConfig]
  )(using config: GeminiConfig): F[Either[GeminiError, GenerateContentResponse]] = {
    val request = GenerateContent(
      contents = contents,
      safetySettings = safetySettings,
      generationConfig = generationConfig.orElse(Some(GeminiService.DefaultGenerationConfig)),
      systemInstruction = systemInstruction,
      tools = tools,
      toolConfig = toolConfig
    )

    httpClient.post[GenerateContent, GenerateContentResponse](
      GeminiService.Endpoints.generateContent(),
      request
    )
  }

  override def generateContentStream(
      contents: List[Content],
      safetySettings: Option[List[SafetySetting]],
      generationConfig: Option[GenerationConfig],
      systemInstruction: Option[Content],
      tools: Option[List[Tool]],
      toolConfig: Option[ToolConfig]
  )(using config: GeminiConfig): Stream[F, GenerateContentResponse] = {
    val request = GenerateContent(
      contents = contents,
      safetySettings = safetySettings,
      generationConfig = generationConfig.orElse(Some(GeminiService.DefaultGenerationConfig)),
      systemInstruction = systemInstruction,
      tools = tools,
      toolConfig = toolConfig
    )

    httpClient.postStream[GenerateContent, GenerateContentResponse](
      GeminiService.Endpoints.generateContentStream(),
      request
    )
  }

  override def countTokens(
      contents: List[Content]
  )(using config: GeminiConfig): F[Either[GeminiError, Int]] = {
    val request = CountTokensRequest(contents)

    httpClient
      .post[CountTokensRequest, CountTokensResponse](
        GeminiService.Endpoints.countTokens(),
        request
      )
      .map(_.map(_.totalTokens))
  }

  override def embedContent(
      content: Content,
      taskType: Option[TaskType],
      title: Option[String],
      outputDimensionality: Option[Int]
  )(using config: GeminiConfig): F[Either[GeminiError, ContentEmbedding]] = {
    val request = EmbedContentRequest(
      content = content,
      model = s"models/${GeminiService.EmbeddingText004}",
      taskType = taskType,
      title = title,
      outputDimensionality = outputDimensionality
    )

    httpClient
      .post[EmbedContentRequest, EmbedContentResponse](
        GeminiService.Endpoints.embedContent(GeminiService.EmbeddingText004),
        request
      )
      .map(_.map(_.embedding))
  }

  override def batchEmbedContents(
      requests: List[EmbedContentRequest]
  )(using config: GeminiConfig): F[Either[GeminiError, List[ContentEmbedding]]] = {
    val request = BatchEmbedContentsRequest(requests)

    httpClient
      .post[BatchEmbedContentsRequest, BatchEmbedContentsResponse](
        GeminiService.Endpoints.batchEmbedContents(GeminiService.EmbeddingText004),
        request
      )
      .map(_.map(_.embeddings))
  }

  override def createCachedContent(
      model: String,
      systemInstruction: Option[Content],
      contents: Option[List[Content]],
      tools: Option[List[Tool]],
      toolConfig: Option[ToolConfig],
      ttl: Option[String],
      displayName: Option[String]
  )(using config: GeminiConfig): F[Either[GeminiError, CachedContent]] = {
    val request = CreateCachedContentRequest(
      model = Some(model),
      systemInstruction = systemInstruction,
      contents = contents,
      tools = tools,
      toolConfig = toolConfig,
      ttl = ttl,
      displayName = displayName
    )

    httpClient.post[CreateCachedContentRequest, CachedContent](
      GeminiService.Endpoints.createCachedContent,
      request
    )
  }

}

object GeminiServiceImpl {

  /**
   * Creates a new GeminiService instance.
   */
  def make[F[_]: Async](httpClient: GeminiHttpClient[F]): GeminiService[F] = new GeminiServiceImpl(httpClient)
}
