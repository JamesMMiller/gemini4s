package gemini4s.model.domain

import io.circe._

/**
 * Enum for model names.
 * Provides type safety and discoverability for model identifiers.
 */
enum ModelName(val value: String) {
  case Gemini25Flash     extends ModelName("gemini-2.5-flash")
  case Gemini25Pro       extends ModelName("gemini-2.5-pro")
  case Gemini25FlashLite extends ModelName("gemini-2.5-flash-lite")
  case Gemini3Pro        extends ModelName("gemini-3-pro-preview")
  case Imagen4           extends ModelName("imagen-4.0-generate-001")
  case EmbeddingText001  extends ModelName("gemini-embedding-001")

  case Custom(override val value: String) extends ModelName(value)
}

object ModelName {

  // Default model
  val Default: ModelName = Gemini25Flash

  val knownValues: List[ModelName] = List(
    Gemini25Flash,
    Gemini25Pro,
    Gemini25FlashLite,
    Gemini3Pro,
    Imagen4,
    EmbeddingText001
  )

  /**
   * Smart constructor for model names.
   *
   * @param value The model name string
   * @return Either an error message or a valid ModelName
   */
  def apply(value: String): Either[String, ModelName] =
    if (value.trim.isEmpty) Left("Model name cannot be empty")
    else {
      val cleanValue = value.stripPrefix("models/")
      Right(knownValues.find(_.value == cleanValue).getOrElse(Custom(cleanValue)))
    }

  /**
   * Unsafe constructor for cases where validation is not needed.
   *
   * @param value The model name string
   * @return A valid ModelName (throws if empty)
   */
  def unsafe(value: String): ModelName = apply(value).fold(err => throw new IllegalArgumentException(err), identity)

  // Circe codecs
  given Encoder[ModelName] = Encoder[String].contramap { model =>
    if (model.value.startsWith("models/")) model.value else s"models/${model.value}"
  }

  given Decoder[ModelName] = Decoder[String].emap(apply)
}
