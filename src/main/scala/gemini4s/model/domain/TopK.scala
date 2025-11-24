package gemini4s.model.domain

import io.circe._

/**
 * Opaque type for topK with validation.
 * TopK must be a positive integer.
 */
opaque type TopK = Int

object TopK {

  /**
   * Smart constructor that validates topK is positive.
   *
   * @param value The topK value
   * @return Either an error message or a valid TopK
   */
  def apply(value: Int): Either[String, TopK] =
    if (value <= 0) Left(s"TopK must be positive, got $value")
    else Right(value)

  /**
   * Unsafe constructor for cases where validation is not needed.
   *
   * @param value The topK value
   * @return A valid TopK (throws if non-positive)
   */
  def unsafe(value: Int): TopK = {
    require(value > 0, s"TopK must be positive, got $value")
    value
  }

  /**
   * Extension methods for TopK.
   */
  extension (k: TopK) {

    /**
     * Get the underlying int value.
     */
    def value: Int = k
  }

  // Circe codecs
  given Encoder[TopK] = Encoder[Int].contramap(_.value)
  given Decoder[TopK] = Decoder[Int].emap(apply)
}
