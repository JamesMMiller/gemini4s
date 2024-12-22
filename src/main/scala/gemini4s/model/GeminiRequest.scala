package gemini4s.model

import zio.json._

/**
 * Base trait for all Gemini API requests.
 */
sealed trait GeminiRequest {
  def toJson: String
}

object GeminiRequest {
  /**
   * Request for text generation using the Gemini API.
   */
  final case class GenerateContent(
    contents: List[Content],
    safetySettings: Option[List[SafetySetting]] = None,
    generationConfig: Option[GenerationConfig] = None
  ) extends GeminiRequest {
    override def toJson: String = {
      val contentJson = contents.map(c => c match {
        case Content.Text(text) => s"""{"parts": [{"text": "$text"}]}"""
      }).mkString("[", ",", "]")

      val safetyJson = safetySettings.map(settings =>
        settings.map(s => s"""{"category": "${s.category}", "threshold": "${s.threshold}"}""").mkString("[", ",", "]")
      ).getOrElse("[]")

      val configJson = generationConfig.map(c =>
        s"""{
           |  "temperature": ${c.temperature.getOrElse("null")},
           |  "topK": ${c.topK.getOrElse("null")},
           |  "topP": ${c.topP.getOrElse("null")},
           |  "candidateCount": ${c.candidateCount.getOrElse("null")},
           |  "maxOutputTokens": ${c.maxOutputTokens.getOrElse("null")},
           |  "stopSequences": ${c.stopSequences.map(_.mkString("[\"", "\",\"", "\"]")).getOrElse("[]")}
           |}""".stripMargin
      ).getOrElse("{}")

      s"""{
         |  "contents": $contentJson,
         |  "safetySettings": $safetyJson,
         |  "generationConfig": $configJson
         |}""".stripMargin
    }
  }

  /**
   * Request to count tokens for given content.
   */
  final case class CountTokensRequest(
    contents: List[Content]
  ) extends GeminiRequest {
    override def toJson: String = {
      val contentJson = contents.map(c => c match {
        case Content.Text(text) => s"""{"parts": [{"text": "$text"}]}"""
      }).mkString("[", ",", "]")

      s"""{"contents": $contentJson}"""
    }
  }

  /**
   * Content part that can be used in requests.
   */
  sealed trait Content
  object Content {
    final case class Text(text: String) extends Content
  }

  /**
   * Safety setting for content filtering.
   */
  final case class SafetySetting(
    category: HarmCategory,
    threshold: HarmBlockThreshold
  )

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

  /**
   * Configuration for text generation.
   */
  final case class GenerationConfig(
    temperature: Option[Float] = None,
    topK: Option[Int] = None,
    topP: Option[Float] = None,
    candidateCount: Option[Int] = None,
    maxOutputTokens: Option[Int] = None,
    stopSequences: Option[List[String]] = None
  )
} 