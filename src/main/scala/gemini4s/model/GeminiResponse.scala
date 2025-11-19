package gemini4s.model

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

object GeminiResponse {

  /**
   * Response from content generation.
   */
  final case class GenerateContentResponse(
      candidates: List[Candidate],
      usageMetadata: Option[UsageMetadata] = None,
      modelVersion: Option[String] = None,
      promptFeedback: Option[PromptFeedback] = None
  )

  object GenerateContentResponse {
    given Decoder[GenerateContentResponse] = deriveDecoder
    given Encoder[GenerateContentResponse] = deriveEncoder
  }

  /**
   * Feedback on the prompt.
   */
  final case class PromptFeedback(
      blockReason: Option[String] = None,
      safetyRatings: Option[List[SafetyRating]] = None
  )

  object PromptFeedback {
    given Decoder[PromptFeedback] = deriveDecoder
    given Encoder[PromptFeedback] = deriveEncoder
  }

  /**
   * Response from token counting.
   */
  final case class CountTokensResponse(
      totalTokens: Int
  )

  object CountTokensResponse {
    given Decoder[CountTokensResponse] = deriveDecoder
    given Encoder[CountTokensResponse] = deriveEncoder
  }

  /**
   * A candidate response from the model.
   */
  final case class Candidate(
      content: ResponseContent,
      finishReason: Option[String] = None,
      index: Option[Int] = None,
      safetyRatings: Option[List[SafetyRating]] = None
  )

  object Candidate {
    given Decoder[Candidate] = deriveDecoder
    given Encoder[Candidate] = deriveEncoder
  }

  /**
   * Content part in a response.
   */
  final case class ResponseContent(
      parts: List[ResponsePart],
      role: Option[String] = None
  )

  object ResponseContent {
    given Decoder[ResponseContent] = deriveDecoder
    given Encoder[ResponseContent] = deriveEncoder
  }

  /**
   * A part of the response content.
   */
  sealed trait ResponsePart

  object ResponsePart {
    final case class Text(text: String) extends ResponsePart
    final case class FunctionCall(functionCall: FunctionCallData) extends ResponsePart
    final case class ExecutableCode(executableCode: ExecutableCodeData) extends ResponsePart
    final case class CodeExecutionResult(codeExecutionResult: CodeExecutionResultData) extends ResponsePart

    // Custom decoder to handle different part types
    given Decoder[ResponsePart] = Decoder.instance { cursor =>
      cursor.downField("text").as[String].map(Text.apply)
        .orElse(cursor.downField("functionCall").as[FunctionCallData].map(FunctionCall.apply))
        .orElse(cursor.downField("executableCode").as[ExecutableCodeData].map(ExecutableCode.apply))
        .orElse(cursor.downField("codeExecutionResult").as[CodeExecutionResultData].map(CodeExecutionResult.apply))
    }

    // Custom encoder
    given Encoder[ResponsePart] = Encoder.instance {
      case Text(text) => Json.obj("text" -> Json.fromString(text))
      case FunctionCall(data) => Json.obj("functionCall" -> data.asJson)
      case ExecutableCode(data) => Json.obj("executableCode" -> data.asJson)
      case CodeExecutionResult(data) => Json.obj("codeExecutionResult" -> data.asJson)
    }
  }

  final case class FunctionCallData(name: String, args: Map[String, Json])
  object FunctionCallData {
    given Decoder[FunctionCallData] = deriveDecoder
    given Encoder[FunctionCallData] = deriveEncoder
  }

  final case class ExecutableCodeData(language: String, code: String)
  object ExecutableCodeData {
    given Decoder[ExecutableCodeData] = deriveDecoder
    given Encoder[ExecutableCodeData] = deriveEncoder
  }

  final case class CodeExecutionResultData(outcome: String, output: String)
  object CodeExecutionResultData {
    given Decoder[CodeExecutionResultData] = deriveDecoder
    given Encoder[CodeExecutionResultData] = deriveEncoder
  }

  /**
   * Safety rating for generated content.
   */
  final case class SafetyRating(
      category: String,
      probability: String
  )

  object SafetyRating {
    given Decoder[SafetyRating] = deriveDecoder
    given Encoder[SafetyRating] = deriveEncoder
  }

  /**
   * Usage metadata for the request.
   */
  final case class UsageMetadata(
      promptTokenCount: Int,
      candidatesTokenCount: Int,
      totalTokenCount: Int
  )

  object UsageMetadata {
    given Decoder[UsageMetadata] = deriveDecoder
    given Encoder[UsageMetadata] = deriveEncoder
  }

}
