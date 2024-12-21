package gemini4s.error

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GeminiErrorSpec extends AnyFlatSpec with Matchers {
  import GeminiError._
  import GeminiErrorCompanion._

  "GeminiError" should "provide correct error messages" in {
    val error = InvalidApiKey("Custom message")
    error.message shouldBe "Custom message"
    error.getMessage shouldBe "Custom message"
  }

  it should "handle causes correctly" in {
    val cause = new RuntimeException("Test cause")
    val error = ConnectionError("Test error", Some(cause))
    error.cause shouldBe Some(cause)
    error.getCause shouldBe cause
  }

  it should "handle null causes correctly" in {
    val error = InvalidRequest("Test error")
    error.cause shouldBe None
    error.getCause shouldBe null
  }

  "GeminiErrorCompanion" should "create errors with smart constructors" in {
    invalidApiKey().message shouldBe "Invalid API key provided"
    missingApiKey.message shouldBe "API key is required but not provided"
    rateLimitExceeded.message shouldBe "API rate limit exceeded"
    modelOverloaded.message shouldBe "Model is currently overloaded"
    timeoutError.message shouldBe "Request timed out"
  }

  it should "create errors with custom messages" in {
    val modelId = "gemini-pro"
    unsupportedModel(modelId).message shouldBe s"Model $modelId is not supported"

    val category = "hate-speech"
    safetyThresholdExceeded(category).message shouldBe
      s"Content exceeded safety threshold for category: $category"

    val msg = "Custom error"
    contentGenerationFailed(msg).message shouldBe msg
  }

  it should "create network errors correctly" in {
    val msg = "Connection failed"
    val cause = new RuntimeException("Network error")
    val error = connectionError(msg, cause)
    error.message shouldBe msg
    error.cause shouldBe Some(cause)
  }

  it should "create stream errors correctly" in {
    val msg = "Stream interrupted"
    val cause = new RuntimeException("Stream error")
    
    val error1 = streamError(msg)
    error1.message shouldBe msg
    error1.cause shouldBe None

    val error2 = streamError(msg, Some(cause))
    error2.message shouldBe msg
    error2.cause shouldBe Some(cause)
  }

  it should "map throwables to GeminiErrors" in {
    val geminiError = InvalidApiKey()
    fromThrowable(geminiError) shouldBe geminiError

    val runtime = new RuntimeException("Test")
    val mapped = fromThrowable(runtime)
    mapped shouldBe a[ConnectionError]
    mapped.message should include("Test")
    mapped.cause shouldBe Some(runtime)
  }

  it should "map status codes to appropriate errors" in {
    val msg = "Test message"
    fromStatusCode(400, msg) shouldBe a[InvalidRequest]
    fromStatusCode(401, msg) shouldBe a[InvalidApiKey]
    fromStatusCode(403, msg) shouldBe a[InvalidApiKey]
    fromStatusCode(404, msg) shouldBe a[InvalidRequest]
    fromStatusCode(429, msg) shouldBe a[RateLimitExceeded]
    fromStatusCode(500, msg) shouldBe a[ModelOverloaded]
    fromStatusCode(503, msg) shouldBe a[ModelOverloaded]
    fromStatusCode(418, msg) shouldBe a[ConnectionError]
  }
} 