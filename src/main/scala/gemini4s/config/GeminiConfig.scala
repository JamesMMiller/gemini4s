package gemini4s.config

import io.circe._

/**
 * Gemini API version with type-safe capability markers.
 *
 * The API versions differ in available features:
 *
 * - **v1beta**: Full feature access - caching, files, Imagen, Veo, AQA
 * - **v1**: Stable subset - basic generation, embeddings, operations
 *
 * Use phantom types to ensure compile-time safety:
 * {{{
 * // This compiles - cachedContents is a v1beta feature
 * def cacheContent[V <: HasCaching](config: VersionedConfig[V]): IO[Unit] = ???
 *
 * // Would NOT compile with V1 config
 * // cacheContent(v1Config)  // Compile error - V1 doesn't have HasCaching
 * }}}
 */
object ApiVersion {

  // ============================================
  // Capability Phantom Types for API Versions
  // ============================================

  /** Marker trait for version capabilities */
  sealed trait VersionCapability

  /** Version supports cachedContents resource */
  trait HasCaching extends VersionCapability

  /** Version supports files resource */
  trait HasFiles extends VersionCapability

  /** Version supports media resource */
  trait HasMedia extends VersionCapability

  /** Version supports generateAnswer method */
  trait HasAQA extends VersionCapability

  /** Version supports predict method (Imagen) */
  trait HasImagen extends VersionCapability

  /** Version supports predictLongRunning method (Veo) */
  trait HasVeo extends VersionCapability

  /** Version supports operations resource */
  trait HasOperations extends VersionCapability

  // ============================================
  // API Version ADT with Capabilities
  // ============================================

  /** v1beta capabilities */
  type V1BetaCapabilities = HasCaching & HasFiles & HasMedia & HasAQA & HasImagen & HasVeo

  /** v1 (stable) capabilities */
  type V1Capabilities = HasOperations

  /**
   * API Version with type-level capability evidence.
   */
  sealed trait Version[C <: VersionCapability] {
    def path: String
  }

  /** v1beta - Full feature access */
  case object V1Beta extends Version[V1BetaCapabilities] {
    val path = "v1beta"
  }

  /** v1 - Stable API with limited features */
  case object V1 extends Version[V1Capabilities] {
    val path = "v1"
  }

  // ============================================
  // Evidence Type Classes
  // ============================================

  /** Evidence that a version supports caching */
  trait SupportsCaching[V]:
    def version: String

  object SupportsCaching:

    given SupportsCaching[V1BetaCapabilities] with
      def version = "v1beta"

  /** Evidence that a version supports file operations */
  trait SupportsFiles[V]:
    def version: String

  object SupportsFiles:

    given SupportsFiles[V1BetaCapabilities] with
      def version = "v1beta"

  /** Evidence that a version supports Imagen */
  trait SupportsImagen[V]:
    def version: String

  object SupportsImagen:

    given SupportsImagen[V1BetaCapabilities] with
      def version = "v1beta"

  /** Evidence that a version supports Veo */
  trait SupportsVeo[V]:
    def version: String

  object SupportsVeo:

    given SupportsVeo[V1BetaCapabilities] with
      def version = "v1beta"

  /** Evidence that a version supports LRO operations */
  trait SupportsOperations[V]:
    def version: String

  object SupportsOperations:

    given SupportsOperations[V1Capabilities] with
      def version = "v1"

  // ============================================
  // Circe Codecs
  // ============================================

  given [C <: VersionCapability]: Encoder[Version[C]] = Encoder[String].contramap(_.path)

  given Decoder[Version[?]] = Decoder[String].emap {
    case "v1beta" => Right(V1Beta)
    case "v1"     => Right(V1)
    case other    => Left(s"Unknown API version: $other. Use 'v1beta' or 'v1'")
  }

  // ============================================
  // Feature availability mappings for audit
  // ============================================

  /** Resources only available in v1beta */
  val v1betaOnlyResources: Set[String] = Set("cachedContents", "files", "media")

  /** Resources only available in v1 */
  val v1OnlyResources: Set[String] = Set("operations")

  /** Methods only available in v1beta */
  val v1betaOnlyMethods: Set[String] = Set(
    "generateAnswer",
    "predict",
    "predictLongRunning",
    // Legacy PaLM methods also only in v1beta
    "generateText",
    "generateMessage",
    "embedText",
    "batchEmbedText",
    "countTextTokens",
    "countMessageTokens"
  )

}

/**
 * Configuration for Gemini API.
 *
 * @param apiKey Your Gemini API key
 * @param apiVersion The API version to use (default: v1beta)
 * @param baseUrl Base URL without version path
 */
final case class GeminiConfig(
    apiKey: String,
    apiVersion: ApiVersion.Version[?] = ApiVersion.V1Beta,
    baseUrl: String = "https://generativelanguage.googleapis.com"
) {

  /** Full base URL including API version */
  def versionedBaseUrl: String = s"$baseUrl/${apiVersion.path}"

  /** API version path (e.g., "v1beta") */
  def versionPath: String = apiVersion.path
}

object GeminiConfig {

  /** Create config for v1beta (full features) */
  def v1beta(apiKey: String): GeminiConfig = GeminiConfig(apiKey, ApiVersion.V1Beta)

  /** Create config for v1 (stable, limited features) */
  def v1(apiKey: String): GeminiConfig = GeminiConfig(apiKey, ApiVersion.V1)
}
