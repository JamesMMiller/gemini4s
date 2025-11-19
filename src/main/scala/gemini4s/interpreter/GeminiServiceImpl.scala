package gemini4s.interpreter

import cats.effect.Async
import cats.syntax.all._
import fs2.Stream

import gemini4s.GeminiService
import gemini4s.GeminiService.Endpoints
import gemini4s.config.GeminiConfig
import gemini4s.error.GeminiError
import gemini4s.http.GeminiHttpClient
import gemini4s.model.GeminiRequest._
import gemini4s.model.GeminiResponse._ // Added import for Endpoints

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
      systemInstruction: Option[Content]
  )(using config: GeminiConfig): F[Either[GeminiError, GenerateContentResponse]] = {
    val request = GenerateContent(
      contents = contents,
      safetySettings = safetySettings,
      generationConfig = generationConfig,
      systemInstruction = systemInstruction
    )
    httpClient.post[GenerateContent, GenerateContentResponse](
      Endpoints.generateContent(GeminiService.DefaultModel),
      request
    )
  }

  override def generateContentStream(
      contents: List[Content],
      safetySettings: Option[List[SafetySetting]],
      generationConfig: Option[GenerationConfig],
      systemInstruction: Option[Content]
  )(using config: GeminiConfig): Stream[F, Either[GeminiError, GenerateContentResponse]] = {
    val request = GenerateContent(
      contents = contents,
      safetySettings = safetySettings,
      generationConfig = generationConfig,
      systemInstruction = systemInstruction
    )
    httpClient
      .postStream[GenerateContent, GenerateContentResponse](
        Endpoints.generateContentStream(GeminiService.DefaultModel),
        request
      )
      .map(Right(_))
  }

  override def countTokens(
      contents: List[Content]
  )(using config: GeminiConfig): F[Either[GeminiError, CountTokensResponse]] = {
    val request = GenerateContent(
      contents = contents,
      safetySettings = None,
      generationConfig = None,
      systemInstruction = None
    )
    httpClient.post[GenerateContent, CountTokensResponse](
      Endpoints.countTokens(GeminiService.DefaultModel),
      request
    )
  }

}

object GeminiServiceImpl {

  def make[F[_]: Async](httpClient: GeminiHttpClient[F]): GeminiService[F] = new GeminiServiceImpl(httpClient)

}
