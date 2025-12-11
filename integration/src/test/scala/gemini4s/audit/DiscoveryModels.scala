package gemini4s.audit

import io.circe._
import io.circe.generic.semiauto._

object DiscoveryModels {

  case class DiscoveryDoc(
      schemas: Option[Map[String, Schema]],
      resources: Option[Map[String, Resource]]
  )

  case class Resource(
      methods: Option[Map[String, Method]],
      resources: Option[Map[String, Resource]]
  )

  case class Method(
      id: String,
      path: String,
      httpMethod: String,
      description: Option[String]
  )

  case class Schema(
      id: Option[String],
      description: Option[String],
      properties: Option[Map[String, SchemaProperty]]
  )

  case class SchemaProperty(
      description: Option[String],
      `type`: Option[String]
  )

  // Circe Decoders
  implicit val schemaPropertyDecoder: Decoder[SchemaProperty] = deriveDecoder
  implicit val schemaDecoder: Decoder[Schema]                 = deriveDecoder
  implicit val methodDecoder: Decoder[Method]                 = deriveDecoder

  // Recursive decoder for Resource
  implicit val resourceDecoder: Decoder[Resource] = deriveDecoder

  implicit val discoveryDocDecoder: Decoder[DiscoveryDoc] = deriveDecoder

  /**
   * Flatten all methods in the discovery document into "resource.method" strings.
   * Handles nested resources recursively.
   */
  def flattenMethods(doc: DiscoveryDoc): Set[String] = {
    def traverse(prefix: String, res: Resource): List[String] = {
      val methods     = res.methods.getOrElse(Map.empty)
      val methodNames = methods.keys.map(m => s"$prefix.$m").toList

      val subs               = res.resources.getOrElse(Map.empty)
      val subResourceMethods = subs.flatMap { case (k, v) => traverse(s"$prefix.$k", v) }
      methodNames ++ subResourceMethods
    }

    doc.resources.getOrElse(Map.empty).flatMap { case (k, v) => traverse(k, v) }.toSet
  }

}
