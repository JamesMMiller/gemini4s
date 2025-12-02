package gemini4s.model.domain

import io.circe._

/**
 * ADT for model names.
 * Provides type safety for model identifiers.
 */
sealed trait ModelName {
  def value: String
}

object ModelName {

  case class Standard(value: String) extends ModelName
  case class Tuned(value: String)    extends ModelName

  // Predefined model constants
  val Gemini25Flash: ModelName      = Standard("gemini-2.5-flash")
  val Gemini25Pro: ModelName        = Standard("gemini-2.5-pro")
  val Gemini25FlashLite: ModelName  = Standard("gemini-2.5-flash-lite")
  val Gemini3Pro: ModelName         = Standard("gemini-3-pro-preview")
  val Imagen4: ModelName            = Standard("imagen-4.0-generate-001")
  val EmbeddingGemini001: ModelName = Standard("gemini-embedding-001")
  val GeminiProLatest: ModelName    = Standard("gemini-pro-latest")
  val GeminiFlashLatest: ModelName  = Standard("gemini-flash-latest")

  // Default model
  val Default: ModelName = GeminiFlashLatest

  /**
   * Smart constructor for custom model names.
   * Defaults to Standard model.
   *
   * @param value The model name string
   * @return Either an error message or a valid ModelName
   */
  def apply(value: String): Either[String, ModelName] =
    if (value.trim.isEmpty) Left("Model name cannot be empty")
    else Right(Standard(value))

  /**
   * Unsafe constructor for cases where validation is not needed.
   *
   * @param value The model name string
   * @return A valid ModelName (throws if empty)
   */
  def unsafe(value: String): ModelName = {
    require(value.trim.nonEmpty, "Model name cannot be empty")
    Standard(value)
  }

  // Circe codecs
  given Encoder[ModelName] = Encoder[String].contramap(_.value)
  given Decoder[ModelName] = Decoder[String].emap(apply)
}
