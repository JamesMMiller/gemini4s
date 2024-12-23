package gemini4s.model

import zio.json._
import zio.json.internal.{RetractReader, Write}

import gemini4s.model.GeminiRequest._
import gemini4s.model.GeminiResponse._

/**
 * JSON codecs for Gemini API models using zio-json.
 */
object GeminiCodecs {
  // Request codecs
  given JsonCodec[Part]               = DeriveJsonCodec.gen[Part]
  given JsonCodec[Content]            = DeriveJsonCodec.gen[Content]
  given JsonCodec[SafetySetting]      = DeriveJsonCodec.gen[SafetySetting]
  given JsonCodec[GenerationConfig]   = DeriveJsonCodec.gen[GenerationConfig]
  given JsonCodec[GenerateContent]    = DeriveJsonCodec.gen[GenerateContent]
  given JsonCodec[CountTokensRequest] = DeriveJsonCodec.gen[CountTokensRequest]

  // Response codecs
  given JsonCodec[ResponsePart]            = DeriveJsonCodec.gen[ResponsePart]
  given JsonCodec[ResponseContent]         = DeriveJsonCodec.gen[ResponseContent]
  given JsonCodec[SafetyRating]            = DeriveJsonCodec.gen[SafetyRating]
  given JsonCodec[UsageMetadata]           = DeriveJsonCodec.gen[UsageMetadata]
  given JsonCodec[Candidate]               = DeriveJsonCodec.gen[Candidate]
  given JsonCodec[GenerateContentResponse] = DeriveJsonCodec.gen[GenerateContentResponse]
  given JsonCodec[CountTokensResponse]     = DeriveJsonCodec.gen[CountTokensResponse]

  // Enum codecs
  given JsonEncoder[HarmCategory] = JsonEncoder[String].contramap[HarmCategory](_.toString)
  given JsonDecoder[HarmCategory] = JsonDecoder[String].map(HarmCategory.valueOf)

  given JsonEncoder[HarmBlockThreshold] = JsonEncoder[String].contramap[HarmBlockThreshold](_.toString)
  given JsonDecoder[HarmBlockThreshold] = JsonDecoder[String].map(HarmBlockThreshold.valueOf)

  // Base trait codec
  given JsonEncoder[GeminiRequest] = new JsonEncoder[GeminiRequest] {

    override def unsafeEncode(a: GeminiRequest, indent: Option[Int], out: Write): Unit = a match {
      case g: GenerateContent    => summon[JsonEncoder[GenerateContent]].unsafeEncode(g, indent, out)
      case c: CountTokensRequest => summon[JsonEncoder[CountTokensRequest]].unsafeEncode(c, indent, out)
    }

  }
}
