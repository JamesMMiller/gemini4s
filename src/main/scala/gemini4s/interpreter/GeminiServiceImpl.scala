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

}

object GeminiServiceImpl {

  /**
   * Creates a new GeminiService instance.
   */
  def make[F[_]: Async](httpClient: GeminiHttpClient[F]): GeminiService[F] = new GeminiServiceImpl(httpClient)
}
