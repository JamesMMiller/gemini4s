package gemini4s.config

import zio.test._

object GeminiConfigSpec extends ZIOSpecDefault {
  def spec = suite("GeminiConfig")(
    test("create with default base URL") {
      val apiKey = "test-api-key"
      val config = GeminiConfig(apiKey)
      assertTrue(
        config.apiKey == apiKey,
        config.baseUrl == GeminiConfig.DefaultBaseUrl
      )
    },

    test("create with custom base URL") {
      val apiKey = "test-api-key"
      val baseUrl = "https://custom.api.endpoint"
      val config = GeminiConfig(apiKey, baseUrl)
      assertTrue(
        config.apiKey == apiKey,
        config.baseUrl == baseUrl
      )
    }
  )
} 