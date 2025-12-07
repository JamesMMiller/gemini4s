package gemini4s.model.domain

import io.circe._

/**
 * Type-safe model capabilities using phantom types.
 *
 * This allows compile-time checking that a model supports a specific operation.
 * For example, you can't call `embedContent` with a model that doesn't support embeddings.
 *
 * Example usage:
 * {{{
 * // These compile
 * val flashModel = Model.gemini25Flash  // Model[CanGenerate & CanCount & CanCache & CanBatch]
 * val embeddingModel = Model.embeddingGemini001  // Model[CanEmbed & CanCount]
 *
 * // This would NOT compile if the method required CanEmbed evidence:
 * // service.embedContent(flashModel, ...)  // Compile error!
 *
 * // This compiles because embeddingModel has CanEmbed:
 * // service.embedContent(embeddingModel, ...)  // OK
 * }}}
 */
object ModelCapabilities {

  // ============================================
  // Capability Phantom Types
  // ============================================

  /** Marker trait for all capabilities */
  sealed trait Capability

  /** Model supports generateContent */
  trait CanGenerate extends Capability

  /**
   * Model supports streamGenerateContent.
   * NOTE: In the API, streaming is implicitly available when generateContent is supported.
   * The :streamGenerateContent endpoint works for any model that supports generateContent,
   * even though it's not explicitly listed in supportedGenerationMethods.
   */
  trait CanStream extends Capability

  /** Model supports embedContent */
  trait CanEmbed extends Capability

  /** Model supports countTokens */
  trait CanCount extends Capability

  /** Model supports createCachedContent */
  trait CanCache extends Capability

  /** Model supports batchGenerateContent */
  trait CanBatch extends Capability

  /** Model supports generateAnswer (AQA) */
  trait CanAnswer extends Capability

  /** Model supports predict (Imagen) */
  trait CanPredict extends Capability

  /** Model supports predictLongRunning (Veo) */
  trait CanPredictLong extends Capability

  // ============================================
  // Type Aliases for Common Capability Combinations
  // ============================================

  /** Standard text generation capabilities */
  type GenerationCapabilities = CanGenerate & CanStream & CanCount

  /** Full generation model capabilities (Flash/Pro models) */
  type FullGenerationCapabilities = GenerationCapabilities & CanCache & CanBatch

  /** Embedding model capabilities */
  type EmbeddingCapabilities = CanEmbed & CanCount

  // ============================================
  // Model with Capabilities
  // ============================================

  /**
   * A model with type-level capability evidence.
   *
   * @tparam C The capabilities this model supports (intersection type)
   * @param name The underlying model name
   */
  final case class Model[C <: Capability](name: ModelName) {

    /** Get the raw model name value */
    def value: String = name.value

    /** Convert to the underlying ModelName for API calls */
    def toModelName: ModelName = name
  }

  object Model {
    // ============================================
    // Pre-defined Models with Their Capabilities
    // ============================================

    /** Gemini 2.5 Flash - Full generation capabilities */
    val gemini25Flash: Model[FullGenerationCapabilities] = Model(ModelName.Gemini25Flash)

    /** Gemini 2.5 Pro - Full generation capabilities */
    val gemini25Pro: Model[FullGenerationCapabilities] = Model(ModelName.Gemini25Pro)

    /** Gemini 2.5 Flash Lite - Full generation capabilities */
    val gemini25FlashLite: Model[FullGenerationCapabilities] = Model(ModelName.Gemini25FlashLite)

    /** Gemini 3 Pro - Full generation capabilities */
    val gemini3Pro: Model[FullGenerationCapabilities] = Model(ModelName.Gemini3Pro)

    /** Gemini Flash Latest - Full generation capabilities */
    val geminiFlashLatest: Model[FullGenerationCapabilities] = Model(ModelName.GeminiFlashLatest)

    /** Gemini Pro Latest - Full generation capabilities */
    val geminiProLatest: Model[FullGenerationCapabilities] = Model(ModelName.GeminiProLatest)

    /** Gemini Embedding 001 - Embedding capabilities */
    val embeddingGemini001: Model[EmbeddingCapabilities] = Model(ModelName.EmbeddingGemini001)

    /** Imagen 4 - Image generation */
    val imagen4: Model[CanPredict] = Model(ModelName.Imagen4)

    // ============================================
    // Smart Constructors
    // ============================================

    /**
     * Create a model with full generation capabilities.
     * Use when you know the model supports all standard operations.
     */
    def generationModel(name: String): Model[FullGenerationCapabilities] = Model(ModelName.Standard(name))

    /**
     * Create a model with embedding capabilities.
     * Use for embedding-specific models.
     */
    def embeddingModel(name: String): Model[EmbeddingCapabilities] = Model(ModelName.Standard(name))

    /**
     * Create a model with minimal capabilities (just generation).
     * Use when you're unsure of the model's full capabilities.
     */
    def basicModel(name: String): Model[CanGenerate] = Model(ModelName.Standard(name))

    // Circe codecs (encode as plain model name)
    given [C <: Capability]: Encoder[Model[C]] = Encoder[ModelName].contramap(_.name)
    given [C <: Capability]: Decoder[Model[C]] = Decoder[ModelName].map(Model(_))
  }

  // ============================================
  // Evidence Type Classes
  // ============================================

  /**
   * Evidence that a model supports content generation.
   * Methods requiring this evidence can only be called with capable models.
   */
  trait SupportsGeneration[M]:
    def toModelName(m: M): ModelName

  object SupportsGeneration:

    given [C <: CanGenerate]: SupportsGeneration[Model[C]] with
      def toModelName(m: Model[C]): ModelName = m.name

    // Allow raw ModelName for backwards compatibility
    given SupportsGeneration[ModelName] with
      def toModelName(m: ModelName): ModelName = m

  /**
   * Evidence that a model supports embeddings.
   */
  trait SupportsEmbedding[M]:
    def toModelName(m: M): ModelName

  object SupportsEmbedding:

    given [C <: CanEmbed]: SupportsEmbedding[Model[C]] with
      def toModelName(m: Model[C]): ModelName = m.name

    given SupportsEmbedding[ModelName] with
      def toModelName(m: ModelName): ModelName = m

  /**
   * Evidence that a model supports token counting.
   */
  trait SupportsTokenCount[M]:
    def toModelName(m: M): ModelName

  object SupportsTokenCount:

    given [C <: CanCount]: SupportsTokenCount[Model[C]] with
      def toModelName(m: Model[C]): ModelName = m.name

    given SupportsTokenCount[ModelName] with
      def toModelName(m: ModelName): ModelName = m

  /**
   * Evidence that a model supports caching.
   */
  trait SupportsCaching[M]:
    def toModelName(m: M): ModelName

  object SupportsCaching:

    given [C <: CanCache]: SupportsCaching[Model[C]] with
      def toModelName(m: Model[C]): ModelName = m.name

    given SupportsCaching[ModelName] with
      def toModelName(m: ModelName): ModelName = m

  /**
   * Evidence that a model supports batch operations.
   */
  trait SupportsBatch[M]:
    def toModelName(m: M): ModelName

  object SupportsBatch:

    given [C <: CanBatch]: SupportsBatch[Model[C]] with
      def toModelName(m: Model[C]): ModelName = m.name

    given SupportsBatch[ModelName] with
      def toModelName(m: ModelName): ModelName = m

}
