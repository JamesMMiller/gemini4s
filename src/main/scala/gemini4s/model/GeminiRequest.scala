package gemini4s.model

import io.circe._
import io.circe.generic.semiauto._

/**
 * Base trait for all Gemini API requests.
 */
trait GeminiRequest

object GeminiRequest {

  given Encoder[GeminiRequest] = Encoder.instance {
    case req: GenerateContent              => deriveEncoder[GenerateContent].apply(req)
    case req: CountTokensRequest           => deriveEncoder[CountTokensRequest].apply(req)
    case req: EmbedContentRequest          => deriveEncoder[EmbedContentRequest].apply(req)
    case req: BatchEmbedContentsRequest    => deriveEncoder[BatchEmbedContentsRequest].apply(req)
    case req: CreateCachedContentRequest   => deriveEncoder[CreateCachedContentRequest].apply(req)
  }

  /**
   * Request for text generation using the Gemini API.
   */
  final case class GenerateContent(
      contents: List[Content],
      safetySettings: Option[List[SafetySetting]] = None,
      generationConfig: Option[GenerationConfig] = None,
      systemInstruction: Option[Content] = None,
      tools: Option[List[Tool]] = None,
      toolConfig: Option[ToolConfig] = None
  ) extends GeminiRequest

  object GenerateContent {
    given Encoder[GenerateContent] = deriveEncoder
    given Decoder[GenerateContent] = deriveDecoder
  }

  /**
   * Request to count tokens for given content.
   */
  final case class CountTokensRequest(
      contents: List[Content]
  ) extends GeminiRequest

  object CountTokensRequest {
    given Encoder[CountTokensRequest] = deriveEncoder
    given Decoder[CountTokensRequest] = deriveDecoder
  }

  /**
   * Request to embed content.
   */
  final case class EmbedContentRequest(
      content: Content,
      model: String,
      taskType: Option[TaskType] = None,
      title: Option[String] = None,
      outputDimensionality: Option[Int] = None
  ) extends GeminiRequest

  object EmbedContentRequest {
    given Encoder[EmbedContentRequest] = deriveEncoder
    given Decoder[EmbedContentRequest] = deriveDecoder
  }

  /**
   * Request to batch embed contents.
   */
  final case class BatchEmbedContentsRequest(
      requests: List[EmbedContentRequest]
  ) extends GeminiRequest

  object BatchEmbedContentsRequest {
    given Encoder[BatchEmbedContentsRequest] = deriveEncoder
    given Decoder[BatchEmbedContentsRequest] = deriveDecoder
  }

  /**
   * Type of task for which the embedding will be used.
   */
  enum TaskType {
    case TASK_TYPE_UNSPECIFIED, RETRIEVAL_QUERY, RETRIEVAL_DOCUMENT, SEMANTIC_SIMILARITY, CLASSIFICATION, CLUSTERING
  }

  object TaskType {
    given Encoder[TaskType] = Encoder.encodeString.contramap(_.toString)
    given Decoder[TaskType] = Decoder.decodeString.emap { str =>
      try Right(TaskType.valueOf(str))
      catch { case _: IllegalArgumentException => Left(s"Unknown TaskType: $str") }
    }
  }

  /**
   * Request to create cached content.
   */
  final case class CreateCachedContentRequest(
      model: Option[String] = None,
      systemInstruction: Option[Content] = None,
      contents: Option[List[Content]] = None,
      tools: Option[List[Tool]] = None,
      toolConfig: Option[ToolConfig] = None,
      ttl: Option[String] = None,
      displayName: Option[String] = None
  ) extends GeminiRequest

  object CreateCachedContentRequest {
    given Encoder[CreateCachedContentRequest] = deriveEncoder
    given Decoder[CreateCachedContentRequest] = deriveDecoder
  }

  /**
   * Content part that can be used in requests.
   */
  final case class Content(
      parts: List[Part],
      role: Option[String] = None
  )

  object Content {
    given Encoder[Content] = deriveEncoder
    given Decoder[Content] = deriveDecoder
  }

  /**
   * A part of the content.
   */
  final case class Part(text: String)

  object Part {
    given Encoder[Part] = deriveEncoder
    given Decoder[Part] = deriveDecoder
  }

  /**
   * Safety setting for content filtering.
   */
  final case class SafetySetting(
      category: HarmCategory,
      threshold: HarmBlockThreshold
  )

  object SafetySetting {
    given Encoder[SafetySetting] = deriveEncoder
    given Decoder[SafetySetting] = deriveDecoder
  }

  /**
   * Categories of potential harm in generated content.
   */
  enum HarmCategory {
    case HARM_CATEGORY_UNSPECIFIED
    case HARASSMENT
    case HATE_SPEECH
    case SEXUALLY_EXPLICIT
    case DANGEROUS_CONTENT
  }

  object HarmCategory {
    given Encoder[HarmCategory] = Encoder.encodeString.contramap(_.toString)

    given Decoder[HarmCategory] = Decoder.decodeString.emap { str =>
      try Right(HarmCategory.valueOf(str))
      catch { case _: IllegalArgumentException => Left(s"Unknown HarmCategory: $str") }
    }

  }

  /**
   * Thresholds for blocking harmful content.
   */
  enum HarmBlockThreshold {
    case HARM_BLOCK_THRESHOLD_UNSPECIFIED
    case BLOCK_LOW_AND_ABOVE
    case BLOCK_MEDIUM_AND_ABOVE
    case BLOCK_ONLY_HIGH
    case BLOCK_NONE
  }

  object HarmBlockThreshold {
    given Encoder[HarmBlockThreshold] = Encoder.encodeString.contramap(_.toString)

    given Decoder[HarmBlockThreshold] = Decoder.decodeString.emap { str =>
      try Right(HarmBlockThreshold.valueOf(str))
      catch { case _: IllegalArgumentException => Left(s"Unknown HarmBlockThreshold: $str") }
    }

  }

  /**
   * Configuration for text generation.
   */
  final case class GenerationConfig(
      temperature: Option[Float] = None,
      topK: Option[Int] = None,
      topP: Option[Float] = None,
      candidateCount: Option[Int] = None,
      maxOutputTokens: Option[Int] = None,
      stopSequences: Option[List[String]] = None,
      responseMimeType: Option[String] = None
  )

  object GenerationConfig {
    given Encoder[GenerationConfig] = deriveEncoder
    given Decoder[GenerationConfig] = deriveDecoder
  }

  /**
   * Tool details that the model may use to generate response.
   */
  final case class Tool(
      functionDeclarations: Option[List[FunctionDeclaration]] = None
  )

  object Tool {
    given Encoder[Tool] = deriveEncoder
    given Decoder[Tool] = deriveDecoder
  }

  /**
   * Configuration for tool use.
   */
  final case class ToolConfig(
      functionCallingConfig: Option[FunctionCallingConfig] = None
  )

  object ToolConfig {
    given Encoder[ToolConfig] = deriveEncoder
    given Decoder[ToolConfig] = deriveDecoder
  }

  /**
   * Configuration for function calling.
   */
  final case class FunctionCallingConfig(
      mode: Option[FunctionCallingMode] = None,
      allowedFunctionNames: Option[List[String]] = None
  )

  object FunctionCallingConfig {
    given Encoder[FunctionCallingConfig] = deriveEncoder
    given Decoder[FunctionCallingConfig] = deriveDecoder
  }

  enum FunctionCallingMode {
    case MODE_UNSPECIFIED
    case AUTO
    case ANY
    case NONE
  }

  object FunctionCallingMode {
    given Encoder[FunctionCallingMode] = Encoder.encodeString.contramap(_.toString)
    given Decoder[FunctionCallingMode] = Decoder.decodeString.emap { str =>
      try Right(FunctionCallingMode.valueOf(str))
      catch { case _: IllegalArgumentException => Left(s"Unknown FunctionCallingMode: $str") }
    }
  }

  /**
   * Structured representation of a function declaration.
   */
  final case class FunctionDeclaration(
      name: String,
      description: String,
      parameters: Option[Schema] = None
  )

  object FunctionDeclaration {
    given Encoder[FunctionDeclaration] = deriveEncoder
    given Decoder[FunctionDeclaration] = deriveDecoder
  }

  /**
   * Schema for function parameters (simplified).
   */
  final case class Schema(
      `type`: SchemaType,
      format: Option[String] = None,
      description: Option[String] = None,
      nullable: Option[Boolean] = None,
      `enum`: Option[List[String]] = None,
      properties: Option[Map[String, Schema]] = None,
      required: Option[List[String]] = None
  )

  object Schema {
    given Encoder[Schema] = deriveEncoder
    given Decoder[Schema] = deriveDecoder
  }

  enum SchemaType {
    case TYPE_UNSPECIFIED, STRING, NUMBER, INTEGER, BOOLEAN, ARRAY, OBJECT
  }

  object SchemaType {
    given Encoder[SchemaType] = Encoder.encodeString.contramap(_.toString)
    given Decoder[SchemaType] = Decoder.decodeString.emap { str =>
      try Right(SchemaType.valueOf(str))
      catch { case _: IllegalArgumentException => Left(s"Unknown SchemaType: $str") }
    }
  }

}
