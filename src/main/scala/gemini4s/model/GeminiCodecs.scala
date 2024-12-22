package gemini4s.model

import zio.json._

import gemini4s.model.GeminiRequest._
import gemini4s.model.GeminiResponse._

/**
 * JSON codecs for Gemini API models using zio-json.
 */
object GeminiCodecs {
  // Content codecs
  given JsonEncoder[Content] = DeriveJsonEncoder.gen[Content]
  given JsonDecoder[Content] = DeriveJsonDecoder.gen[Content]

  // Part codecs
  given JsonEncoder[Part] = DeriveJsonEncoder.gen[Part]
  given JsonDecoder[Part] = DeriveJsonDecoder.gen[Part]

  // Response content codecs
  given JsonEncoder[ResponseContent] = DeriveJsonEncoder.gen[ResponseContent]
  given JsonDecoder[ResponseContent] = DeriveJsonDecoder.gen[ResponseContent]

  // Safety rating codecs
  given JsonEncoder[SafetyRating] = DeriveJsonEncoder.gen[SafetyRating]
  given JsonDecoder[SafetyRating] = DeriveJsonDecoder.gen[SafetyRating]

  // Usage metadata codecs
  given JsonEncoder[UsageMetadata] = DeriveJsonEncoder.gen[UsageMetadata]
  given JsonDecoder[UsageMetadata] = DeriveJsonDecoder.gen[UsageMetadata]

  // Candidate codecs
  given JsonEncoder[Candidate] = DeriveJsonEncoder.gen[Candidate]
  given JsonDecoder[Candidate] = DeriveJsonDecoder.gen[Candidate]

  // Request codecs
  given generateContentCodec: JsonCodec[GenerateContent] = DeriveJsonCodec.gen[GenerateContent]
  given countTokensRequestCodec: JsonCodec[CountTokensRequest] = DeriveJsonCodec.gen[CountTokensRequest]

  // Response codecs
  given generateContentResponseCodec: JsonCodec[GenerateContentResponse] = DeriveJsonCodec.gen[GenerateContentResponse]
  given countTokensResponseCodec: JsonCodec[CountTokensResponse] = DeriveJsonCodec.gen[CountTokensResponse]

  // Other codecs
  given JsonCodec[SafetySetting] = DeriveJsonCodec.gen[SafetySetting]
  given JsonCodec[GenerationConfig] = DeriveJsonCodec.gen[GenerationConfig]
  given JsonCodec[HarmCategory] = DeriveJsonCodec.gen[HarmCategory]
  given JsonCodec[HarmBlockThreshold] = DeriveJsonCodec.gen[HarmBlockThreshold]
} 