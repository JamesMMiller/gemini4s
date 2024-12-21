package gemini4s.error

import zio.test.Assertion._
import zio.test._

object GeminiErrorSpec extends ZIOSpecDefault {
  import GeminiError._
  import GeminiErrorCompanion._

  def spec = suite("GeminiError")(
    test("provide correct error messages") {
      val error = InvalidApiKey("Custom message")
      assertTrue(
        error.message == "Custom message",
        error.getMessage == "Custom message"
      )
    },

    test("handle causes correctly") {
      val cause = new RuntimeException("Test cause")
      val error = ConnectionError("Test error", Some(cause))
      assertTrue(
        error.cause == Some(cause),
        error.getCause == cause
      )
    },

    test("handle null causes correctly") {
      val error = InvalidRequest("Test error")
      assertTrue(
        error.cause == None,
        error.getCause == null
      )
    },

    test("create errors with smart constructors") {
      assertTrue(
        invalidApiKey().message == "Invalid API key provided",
        missingApiKey.message == "API key is required but not provided",
        rateLimitExceeded.message == "API rate limit exceeded",
        modelOverloaded.message == "Model is currently overloaded",
        timeoutError.message == "Request timed out"
      )
    },

    test("create errors with custom messages") {
      val modelId = "gemini-pro"
      val category = "hate-speech"
      val msg = "Custom error"
      assertTrue(
        unsupportedModel(modelId).message == s"Model $modelId is not supported",
        safetyThresholdExceeded(category).message == s"Content exceeded safety threshold for category: $category",
        contentGenerationFailed(msg).message == msg
      )
    },

    test("create network errors correctly") {
      val msg = "Connection failed"
      val cause = new RuntimeException("Network error")
      val error = connectionError(msg, cause)
      assertTrue(
        error.message == msg,
        error.cause == Some(cause)
      )
    },

    test("create stream errors correctly") {
      val msg = "Stream interrupted"
      val cause = new RuntimeException("Stream error")
      
      val error1 = streamError(msg)
      val error2 = streamError(msg, Some(cause))
      assertTrue(
        error1.message == msg,
        error1.cause == None,
        error2.message == msg,
        error2.cause == Some(cause)
      )
    },

    test("map throwables to GeminiErrors") {
      val geminiError = InvalidApiKey()
      val runtime = new RuntimeException("Test")
      val mapped = fromThrowable(runtime)
      assertTrue(
        fromThrowable(geminiError) == geminiError,
        mapped.isInstanceOf[ConnectionError],
        mapped.message.contains("Test"),
        mapped.cause == Some(runtime)
      )
    },

    test("map status codes to appropriate errors") {
      val msg = "Test message"
      assertTrue(
        fromStatusCode(400, msg).isInstanceOf[InvalidRequest],
        fromStatusCode(401, msg).isInstanceOf[InvalidApiKey],
        fromStatusCode(403, msg).isInstanceOf[InvalidApiKey],
        fromStatusCode(404, msg).isInstanceOf[InvalidRequest],
        fromStatusCode(429, msg).isInstanceOf[RateLimitExceeded],
        fromStatusCode(500, msg).isInstanceOf[ModelOverloaded],
        fromStatusCode(503, msg).isInstanceOf[ModelOverloaded],
        fromStatusCode(418, msg).isInstanceOf[ConnectionError]
      )
    }
  )
} 