package gemini4s.model.domain

import io.circe._

/**
 * Opaque type for MIME types.
 * Provides type safety for response MIME type specifications.
 */
opaque type MimeType = String

object MimeType {

  // Predefined MIME type constants
  val TextPlain: MimeType       = "text/plain"
  val ApplicationJson: MimeType = "application/json"
  val TextHtml: MimeType        = "text/html"
  val TextMarkdown: MimeType    = "text/markdown"

  /**
   * Smart constructor for custom MIME types.
   *
   * @param value The MIME type string
   * @return Either an error message or a valid MimeType
   */
  def apply(value: String): Either[String, MimeType] =
    if (value.trim.isEmpty) Left("MIME type cannot be empty")
    else if (!value.contains("/")) Left(s"Invalid MIME type format: $value")
    else Right(value)

  /**
   * Unsafe constructor for cases where validation is not needed.
   *
   * @param value The MIME type string
   * @return A valid MimeType (throws if invalid)
   */
  def unsafe(value: String): MimeType = {
    require(value.trim.nonEmpty && value.contains("/"), s"Invalid MIME type: $value")
    value
  }

  /**
   * Extension methods for MimeType.
   */
  extension (mimeType: MimeType) {

    /**
     * Get the underlying string value.
     */
    def value: String = mimeType
  }

  // Circe codecs
  given Encoder[MimeType] = Encoder[String].contramap(_.value)
  given Decoder[MimeType] = Decoder[String].emap(apply)
}
