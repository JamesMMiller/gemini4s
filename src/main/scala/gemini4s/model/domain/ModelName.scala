package gemini4s.model.domain

import io.circe._

/**
 * Opaque type for model names.
 * Provides type safety for model identifiers.
 */
opaque type ModelName = String

object ModelName {

  // Predefined model constants
  val Gemini25Flash: ModelName      = "gemini-2.5-flash"
  val Gemini25Pro: ModelName        = "gemini-2.5-pro"
  val Gemini25FlashLite: ModelName  = "gemini-2.5-flash-lite"
  val Gemini3Pro: ModelName         = "gemini-3-pro-preview"
  val Imagen4: ModelName            = "imagen-4.0-generate-001"
  val EmbeddingGemini001: ModelName = "gemini-embedding-001"
  val GeminiProLatest: ModelName    = "gemini-pro-latest"
  val GeminiFlashLatest: ModelName  = "gemini-flash-latest"

  // Default model
  val Default: ModelName = GeminiFlashLatest

  /**
   * Smart constructor for custom model names.
   *
   * @param value The model name string
   * @return Either an error message or a valid ModelName
   */
  def apply(value: String): Either[String, ModelName] =
    if (value.trim.isEmpty) Left("Model name cannot be empty")
    else Right(value)

  /**
   * Unsafe constructor for cases where validation is not needed.
   *
   * @param value The model name string
   * @return A valid ModelName (throws if empty)
   */
  def unsafe(value: String): ModelName = {
    require(value.trim.nonEmpty, "Model name cannot be empty")
    value
  }

  /**
   * Extension methods for ModelName.
   */
  extension (name: ModelName) {

    /**
     * Get the underlying string value.
     */
    def value: String = name

    /**
     * Get the API-compatible string value (with "models/" prefix).
     */
    def toApiString: String =
      if (name.startsWith("models/") || name.startsWith("tunedModels/")) name
      else s"models/$name"

  }

  // Circe codecs
  given Encoder[ModelName] = Encoder[String].contramap(_.value)
  given Decoder[ModelName] = Decoder[String].emap(apply)
}
