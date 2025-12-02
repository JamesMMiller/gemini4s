package gemini4s.model.domain

import io.circe._
import io.circe.generic.semiauto._

/**
 * Tool details that the model may use to generate response.
 */
final case class Tool(
    functionDeclarations: Option[List[FunctionDeclaration]] = None,
    codeExecution: Option[CodeExecution] = None
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
    required: Option[List[String]] = None,
    items: Option[Schema] = None
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
