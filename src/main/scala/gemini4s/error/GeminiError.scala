package gemini4s.error

/**
 * Base trait for all Gemini API errors.
 * Follows the error model defined in the API Design Guide:
 * https://cloud.google.com/apis/design/errors
 */
sealed trait GeminiError extends Throwable {
  def message: String
  def cause: Option[Throwable]
  override def getMessage: String = message
  override def getCause: Throwable = cause.orNull
}

object GeminiError {
  /**
   * Authentication errors related to API key issues
   */
  sealed trait AuthError extends GeminiError

  final case class InvalidApiKey(
    message: String = "Invalid API key provided",
    cause: Option[Throwable] = None
  ) extends AuthError

  final case class MissingApiKey(
    message: String = "API key is required but not provided",
    cause: Option[Throwable] = None
  ) extends AuthError

  /**
   * Request validation and rate limiting errors
   */
  sealed trait RequestError extends GeminiError

  final case class RateLimitExceeded(
    message: String = "API rate limit exceeded",
    cause: Option[Throwable] = None
  ) extends RequestError

  final case class InvalidRequest(
    message: String,
    cause: Option[Throwable] = None
  ) extends RequestError

  /**
   * Model-specific errors
   */
  sealed trait ModelError extends GeminiError

  final case class UnsupportedModel(
    message: String,
    cause: Option[Throwable] = None
  ) extends ModelError

  final case class ModelOverloaded(
    message: String = "Model is currently overloaded",
    cause: Option[Throwable] = None
  ) extends ModelError

  /**
   * Content generation errors
   */
  sealed trait ContentError extends GeminiError

  final case class SafetyThresholdExceeded(
    message: String,
    cause: Option[Throwable] = None
  ) extends ContentError

  final case class ContentGenerationFailed(
    message: String,
    cause: Option[Throwable] = None
  ) extends ContentError

  /**
   * Network and transport errors
   */
  sealed trait NetworkError extends GeminiError

  final case class ConnectionError(
    message: String,
    cause: Option[Throwable]
  ) extends NetworkError

  final case class TimeoutError(
    message: String = "Request timed out",
    cause: Option[Throwable] = None
  ) extends NetworkError

  /**
   * Streaming errors
   */
  sealed trait StreamError extends GeminiError

  final case class StreamInitializationError(
    message: String,
    cause: Option[Throwable] = None
  ) extends StreamError

  final case class StreamInterrupted(
    message: String,
    cause: Option[Throwable] = None
  ) extends StreamError
} 