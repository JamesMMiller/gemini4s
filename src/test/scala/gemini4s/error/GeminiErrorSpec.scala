package gemini4s.error

import munit.FunSuite

class GeminiErrorSpec extends FunSuite {
  import GeminiError._
  import GeminiErrorCompanion._

  test("provide correct error messages") {
    val error = InvalidApiKey("Custom message")
    assertEquals(error.message, "Custom message")
    assertEquals(error.getMessage, "Custom message")
  }

  test("handle causes correctly") {
    val cause = new RuntimeException("Test cause")
    val error = ConnectionError("Test error", Some(cause))
    assertEquals(error.cause, Some(cause))
    assertEquals(error.getCause, cause)
  }

  test("handle null causes correctly") {
    val error = InvalidRequest("Test error")
    assertEquals(error.cause, None)
    assertEquals(error.getCause, null)
  }

  test("create errors with smart constructors") {
    assertEquals(invalidApiKey().message, "Invalid API key provided")
    assertEquals(missingApiKey.message, "API key is required but not provided")
    assertEquals(rateLimitExceeded.message, "API rate limit exceeded")
    assertEquals(modelOverloaded.message, "Model is currently overloaded")
    assertEquals(timeoutError.message, "Request timed out")
  }

  test("create errors with custom messages") {
    val modelId  = "gemini-pro"
    val category = "hate-speech"
    val msg      = "Custom error"
    assertEquals(unsupportedModel(modelId).message, s"Model $modelId is not supported")
    assertEquals(
      safetyThresholdExceeded(category).message,
      s"Content exceeded safety threshold for category: $category"
    )
    assertEquals(contentGenerationFailed(msg).message, msg)
  }

  test("create network errors correctly") {
    val msg   = "Connection failed"
    val cause = new RuntimeException("Network error")
    val error = connectionError(msg, cause)
    assertEquals(error.message, msg)
    assertEquals(error.cause, Some(cause))
  }

  test("create stream errors correctly") {
    val msg   = "Stream interrupted"
    val cause = new RuntimeException("Stream error")

    val error1 = streamError(msg)
    val error2 = streamError(msg, Some(cause))

    assertEquals(error1.message, msg)
    assertEquals(error1.cause, None)
    assertEquals(error2.message, msg)
    assertEquals(error2.cause, Some(cause))
  }

  test("map throwables to GeminiErrors") {
    val geminiError = InvalidApiKey()
    val runtime     = new RuntimeException("Test")
    val mapped      = fromThrowable(runtime)

    assertEquals(fromThrowable(geminiError), geminiError)
    assert(mapped.isInstanceOf[ConnectionError])
    assert(mapped.message.contains("Test"))
    assertEquals(mapped.cause, Some(runtime))
  }

  test("map status codes to appropriate errors") {
    val msg = "Test message"
    assert(fromStatusCode(400, msg).isInstanceOf[InvalidRequest])
    assert(fromStatusCode(401, msg).isInstanceOf[InvalidApiKey])
    assert(fromStatusCode(403, msg).isInstanceOf[InvalidApiKey])
    assert(fromStatusCode(404, msg).isInstanceOf[InvalidRequest])
    assert(fromStatusCode(429, msg).isInstanceOf[RateLimitExceeded])
    assert(fromStatusCode(500, msg).isInstanceOf[ModelOverloaded])
    assert(fromStatusCode(503, msg).isInstanceOf[ModelOverloaded])
    assert(fromStatusCode(418, msg).isInstanceOf[ConnectionError])
  }
}
