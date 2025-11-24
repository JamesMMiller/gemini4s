package gemini4s.model.domain

import io.circe._

/**
 * Opaque type for temperature with validation.
 * Temperature must be between 0.0 and 2.0 (inclusive).
 */
opaque type Temperature = Float

object Temperature {

  private val MinValue = 0.0f
  private val MaxValue = 2.0f

  /**
   * Smart constructor that validates temperature is within valid range.
   *
   * @param value The temperature value
   * @return Either an error message or a valid Temperature
   */
  def apply(value: Float): Either[String, Temperature] =
    if (value < MinValue || value > MaxValue) Left(s"Temperature must be between $MinValue and $MaxValue, got $value")
    else Right(value)

  /**
   * Unsafe constructor for cases where validation is not needed.
   *
   * @param value The temperature value
   * @return A valid Temperature (throws if out of range)
   */
  def unsafe(value: Float): Temperature = {
    require(value >= MinValue && value <= MaxValue, s"Temperature must be between $MinValue and $MaxValue, got $value")
    value
  }

  /**
   * Extension methods for Temperature.
   */
  extension (temp: Temperature) {

    /**
     * Get the underlying float value.
     */
    def value: Float = temp
  }

  // Circe codecs
  given Encoder[Temperature] = Encoder[Float].contramap(_.value)
  given Decoder[Temperature] = Decoder[Float].emap(apply)
}
