package gemini4s.audit

import scala.annotation.StaticAnnotation

/**
 * Maps a Scala method to a specific Gemini API Resource and Method.
 *
 * Used by the automated audit to verify that:
 * 1. The code implements the specified API method (e.g. "models.generateContent")
 * 2. The API actually supports this method
 *
 * @param resource The API resource name (e.g., "models", "files", "cachedContents")
 * @param method   The API method name (e.g., "generateContent", "list")
 */
final case class ApiMapping(resource: String, method: String) extends StaticAnnotation
