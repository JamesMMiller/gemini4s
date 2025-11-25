package gemini4s.config

final case class GeminiConfig(
    apiKey: String,
    baseUrl: String = "https://generativelanguage.googleapis.com/v1beta"
)
