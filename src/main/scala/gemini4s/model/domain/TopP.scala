package gemini4s.model.domain

import io.circe._

/**
 * Opaque type for topP with validation.
 * TopP must be between 0.0 (exclusive) and 1.0 (inclusive).
 */
opaque type TopP = Float

object TopP {

  private val MinValue = 0.0f
  private val MaxValue = 1.0f

  /**
   * Smart constructor that validates topP is within valid range.
   *
   * @param value The topP value
   * @return Either an error message or a valid TopP
   */
  def apply(value: Float): Either[String, TopP] =
    if (value <= MinValue || value > MaxValue)
      Left(s"TopP must be between $MinValue (exclusive) and $MaxValue (inclusive), got $value")
    else Right(value)

  /**
   * Unsafe constructor for cases where validation is not needed.
   *
   * @param value The topP value
   * @return A valid TopP (throws if out of range)
   */
  def unsafe(value: Float): TopP = {
    require(
      value > MinValue && value <= MaxValue,
      s"TopP must be between $MinValue (exclusive) and $MaxValue (inclusive), got $value"
    )
    value
  }

  /**
   * Extension methods for TopP.
   */
  extension (p: TopP) {

    /**
     * Get the underlying float value.
     */
    def value: Float = p
  }

  // Circe codecs
  given Encoder[TopP] = Encoder[Float].contramap(_.value)
  given Decoder[TopP] = Decoder[Float].emap(apply)
}
