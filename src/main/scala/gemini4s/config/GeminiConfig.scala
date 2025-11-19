package gemini4s.config

/**
 * Configuration for the Gemini API client.
 * Applications are responsible for providing the API key through their preferred configuration mechanism.
 */
final case class GeminiConfig(
    apiKey: String,
    baseUrl: String = GeminiConfig.DefaultBaseUrl
)

object GeminiConfig {

  /** Default base URL for the Gemini API */
  val DefaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta"
}
