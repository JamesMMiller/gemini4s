package gemini4s.error

import scala.util.control.NonFatal

/**
 * Companion object providing smart constructors and error mapping utilities
 * for GeminiError types.
 */
object GeminiErrorCompanion {
  import GeminiError._

  /**
   * Smart constructors for common error scenarios
   */
  def invalidApiKey(msg: String = "Invalid API key provided"): GeminiError = InvalidApiKey(msg)

  def missingApiKey: GeminiError = MissingApiKey()

  def rateLimitExceeded: GeminiError = RateLimitExceeded()

  def invalidRequest(msg: String): GeminiError = InvalidRequest(msg)

  def modelOverloaded: GeminiError = ModelOverloaded()

  def unsupportedModel(modelId: String): GeminiError = UnsupportedModel(s"Model $modelId is not supported")

  def safetyThresholdExceeded(category: String): GeminiError =
    SafetyThresholdExceeded(s"Content exceeded safety threshold for category: $category")

  def contentGenerationFailed(msg: String): GeminiError = ContentGenerationFailed(msg)

  def connectionError(msg: String, cause: Throwable): GeminiError = ConnectionError(msg, Some(cause))

  def timeoutError: GeminiError = TimeoutError()

  def streamError(msg: String, cause: Option[Throwable] = None): GeminiError = StreamInterrupted(msg, cause)

  /**
   * Error mapping utilities
   */
  def fromThrowable(t: Throwable): GeminiError = t match {
    case e: GeminiError => e
    case NonFatal(e)    => ConnectionError(
        message = s"Unexpected error: ${e.getMessage}",
        cause = Some(e)
      )
  }

  def fromStatusCode(code: Int, msg: String): GeminiError = code match {
    case 400 => InvalidRequest(msg)
    case 401 => InvalidApiKey(msg)
    case 403 => InvalidApiKey(msg)
    case 404 => InvalidRequest(msg)
    case 429 => RateLimitExceeded(msg)
    case 500 => ModelOverloaded(msg)
    case 503 => ModelOverloaded(msg)
    case _   => ConnectionError(s"Unexpected status code $code: $msg", None)
  }

}
