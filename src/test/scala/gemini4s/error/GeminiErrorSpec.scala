package gemini4s.error

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GeminiErrorSpec extends AnyFlatSpec with Matchers {
  "GeminiError" should "provide correct error messages" in {
    val msg = "Test error"
    val error = new GeminiError(msg)
    error.getMessage shouldBe msg
  }

  it should "handle causes correctly" in {
    val msg = "Test error"
    val cause = new RuntimeException("Cause")
    val error = new GeminiError(msg, Some(cause))
    error.getMessage shouldBe msg
    error.cause shouldBe Some(cause)
    error.getCause shouldBe cause
  }

  it should "handle null causes correctly" in {
    val msg = "Test error"
    val error = new GeminiError(msg, None)
    error.getMessage shouldBe msg
    error.cause shouldBe None
    error.getCause shouldBe null
  }

  "GeminiErrorCompanion" should "create errors with smart constructors" in {
    val modelId = "test-model"
    val category = "test-category"
    val msg = "Test error"

    unsupportedModel(modelId).getMessage shouldBe s"Model $modelId is not supported"
    
    safetyThresholdExceeded(category).getMessage shouldBe
      s"Content blocked due to safety threshold exceeded for category: $category"
    
    contentGenerationFailed(msg).getMessage shouldBe msg
  }

  it should "create errors with custom messages" in {
    val msg = "Test error"
    val cause = new RuntimeException("Cause")
    val error = GeminiError(msg, Some(cause))
    error.getMessage shouldBe msg
    error.cause shouldBe Some(cause)
  }

  it should "create network errors correctly" in {
    val msg = "Test error"
    val cause = new RuntimeException("Cause")
    val error1 = networkError(msg)
    val error2 = networkError(msg, Some(cause))

    error1.getMessage shouldBe msg
    error1.cause shouldBe None

    error2.getMessage shouldBe msg
    error2.cause shouldBe Some(cause)
  }

  it should "map throwables to GeminiErrors" in {
    val geminiError = GeminiError("Test")
    val runtime = new RuntimeException("Test")
    
    fromThrowable(geminiError) shouldBe geminiError
    
    val mapped = fromThrowable(runtime)
    mapped shouldBe a[ConnectionError]
    mapped.getMessage should include("Test")
    mapped.cause shouldBe Some(runtime)
  }

  it should "map status codes to appropriate errors" in {
    val msg = "Test error"
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