package gemini4s.config

import io.circe._

/**
 * Opaque type for API key with validation.
 * Ensures that API keys are non-empty strings.
 */
opaque type ApiKey = String

object ApiKey {

  /**
   * Smart constructor that validates the API key is non-empty.
   *
   * @param value The API key string
   * @return Either an error message or a valid ApiKey
   */
  def apply(value: String): Either[String, ApiKey] =
    if (value.trim.isEmpty) Left("API key cannot be empty")
    else Right(value)

  /**
   * Unsafe constructor for testing and cases where validation is not needed.
   * Use with caution.
   *
   * @param value The API key string
   * @return A valid ApiKey (throws if empty)
   */
  def unsafe(value: String): ApiKey = {
    require(value.trim.nonEmpty, "API key cannot be empty")
    value
  }

  /**
   * Extension methods for ApiKey.
   */
  extension (key: ApiKey) {

    /**
     * Get the underlying string value for use in HTTP headers.
     */
    def value: String = key
  }

  // Circe codecs
  given Encoder[ApiKey] = Encoder[String].contramap(_.value)
  given Decoder[ApiKey] = Decoder[String].emap(apply)
}
