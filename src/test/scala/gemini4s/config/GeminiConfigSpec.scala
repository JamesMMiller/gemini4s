package gemini4s.config

import munit.FunSuite

class GeminiConfigSpec extends FunSuite {

  test("create with default base URL") {
    val apiKey = "test-api-key"
    val config = GeminiConfig(apiKey)
    assertEquals(config.apiKey, apiKey)
    assertEquals(config.baseUrl, GeminiConfig.DefaultBaseUrl)
  }

  test("create with custom base URL") {
    val apiKey  = "test-api-key"
    val baseUrl = "https://custom.api.endpoint"
    val config  = GeminiConfig(apiKey, baseUrl)
    assertEquals(config.apiKey, apiKey)
    assertEquals(config.baseUrl, baseUrl)
  }
}
