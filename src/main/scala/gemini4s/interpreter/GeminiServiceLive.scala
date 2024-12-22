package gemini4s.interpreter

import zio._
import zio.json._
import zio.stream.ZStream

import gemini4s.GeminiService
import gemini4s.config.GeminiConfig
import gemini4s.error.{GeminiError, GeminiErrorCompanion}
import gemini4s.http.GeminiHttpClient
import gemini4s.model.GeminiRequest._
import gemini4s.model.GeminiResponse._

/**
 * Live ZIO implementation of the GeminiService.
 * Uses GeminiHttpClient for API communication and handles proper error mapping.
 */
final class GeminiServiceLive(
  httpClient: GeminiHttpClient[Task]
) extends GeminiService[Task] {

  override def generateContent(
    contents: List[Content],
    safetySettings: Option[List[SafetySetting]],
    generationConfig: Option[GenerationConfig]
  )(using config: GeminiConfig): Task[Either[GeminiError, GenerateContentResponse]] = {
    val request = GenerateContent(
      contents = contents,
      safetySettings = safetySettings,
      generationConfig = generationConfig.orElse(Some(GeminiService.DefaultGenerationConfig))
    )

    httpClient.post[GenerateContent, GenerateContentResponse](
      GeminiService.Endpoints.generateContent(),
      request
    )
  }

  override def generateContentStream(
    contents: List[Content],
    safetySettings: Option[List[SafetySetting]],
    generationConfig: Option[GenerationConfig]
  )(using config: GeminiConfig): Task[ZStream[Any, GeminiError, GenerateContentResponse]] = {
    val request = GenerateContent(
      contents = contents,
      safetySettings = safetySettings,
      generationConfig = generationConfig.orElse(Some(GeminiService.DefaultGenerationConfig))
    )

    httpClient.postStream[GenerateContent, GenerateContentResponse](
      GeminiService.Endpoints.generateContentStream(),
      request
    )
  }

  override def countTokens(
    contents: List[Content]
  )(using config: GeminiConfig): Task[Either[GeminiError, Int]] = {
    val request = CountTokensRequest(contents)

    httpClient.post[CountTokensRequest, CountTokensResponse](
      GeminiService.Endpoints.countTokens(),
      request
    ).map(_.map(_.tokenCount))
  }
}

object GeminiServiceLive {
  /**
   * Creates a new GeminiService layer using the provided HTTP client.
   *
   * @param httpClient The HTTP client to use for API communication
   * @return A ZLayer that provides a GeminiService
   */
  def layer(httpClient: GeminiHttpClient[Task]): ULayer[GeminiService[Task]] =
    ZLayer.succeed(new GeminiServiceLive(httpClient))

  /**
   * Creates a new GeminiService layer using the default HTTP client.
   *
   * @return A ZLayer that provides a GeminiService
   */
  val live: URLayer[GeminiHttpClient[Task], GeminiService[Task]] =
    ZLayer {
      for {
        client <- ZIO.service[GeminiHttpClient[Task]]
      } yield new GeminiServiceLive(client)
    }
} 