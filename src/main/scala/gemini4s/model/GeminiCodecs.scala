package gemini4s.model

import zio.json._

import GeminiRequest._
import GeminiResponse._

/**
 * JSON codecs for Gemini API models using zio-json.
 */
object GeminiCodecs {
  // Request codecs
  given contentTextCodec: JsonCodec[Content.Text] = DeriveJsonCodec.gen[Content.Text]

  given contentCodec: JsonCodec[Content] = JsonCodec[Content.Text].transform[Content](
    text => text,
    {
      case text: Content.Text => text
    }
  )

  given safetySettingCodec: JsonCodec[SafetySetting] = DeriveJsonCodec.gen[SafetySetting]

  given generationConfigCodec: JsonCodec[GenerationConfig] = DeriveJsonCodec.gen[GenerationConfig]

  given generateContentCodec: JsonCodec[GenerateContent] = DeriveJsonCodec.gen[GenerateContent]

  // Response codecs
  given responsePartTextCodec: JsonCodec[ResponsePart.Text] = DeriveJsonCodec.gen[ResponsePart.Text]

  given responsePartCodec: JsonCodec[ResponsePart] = JsonCodec[ResponsePart.Text].transform[ResponsePart](
    text => text,
    {
      case text: ResponsePart.Text => text
    }
  )

  given responseContentCodec: JsonCodec[ResponseContent] = DeriveJsonCodec.gen[ResponseContent]

  given safetyRatingCodec: JsonCodec[SafetyRating] = DeriveJsonCodec.gen[SafetyRating]

  given citationCodec: JsonCodec[Citation] = DeriveJsonCodec.gen[Citation]

  given citationMetadataCodec: JsonCodec[CitationMetadata] = DeriveJsonCodec.gen[CitationMetadata]

  given candidateCodec: JsonCodec[Candidate] = DeriveJsonCodec.gen[Candidate]

  given promptFeedbackCodec: JsonCodec[PromptFeedback] = DeriveJsonCodec.gen[PromptFeedback]

  given generateContentResponseCodec: JsonCodec[GenerateContentResponse] = DeriveJsonCodec.gen[GenerateContentResponse]

  // Enum codecs
  given harmCategoryCodec: JsonCodec[HarmCategory] = JsonCodec[String].transform(
    str => HarmCategory.valueOf(str),
    value => value.toString
  )

  given harmBlockThresholdCodec: JsonCodec[HarmBlockThreshold] = JsonCodec[String].transform(
    str => HarmBlockThreshold.valueOf(str),
    value => value.toString
  )

  given finishReasonCodec: JsonCodec[FinishReason] = JsonCodec[String].transform(
    str => FinishReason.valueOf(str),
    value => value.toString
  )

  given harmProbabilityCodec: JsonCodec[HarmProbability] = JsonCodec[String].transform(
    str => HarmProbability.valueOf(str),
    value => value.toString
  )

  given blockReasonCodec: JsonCodec[BlockReason] = JsonCodec[String].transform(
    str => BlockReason.valueOf(str),
    value => value.toString
  )
} 