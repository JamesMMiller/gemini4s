package gemini4s.config

import munit.FunSuite
import io.circe.syntax._
import io.circe.parser._

class ApiVersionSpec extends FunSuite {

  test("V1Beta should have correct path") {
    assertEquals(ApiVersion.V1Beta.path, "v1beta")
  }

  test("V1 should have correct path") {
    assertEquals(ApiVersion.V1.path, "v1")
  }

  test("ApiVersion encoder should encode V1Beta correctly") {
    val version: ApiVersion.Version[ApiVersion.V1BetaCapabilities] = ApiVersion.V1Beta
    val encoded                                                    = version.asJson
    assertEquals(encoded.asString, Some("v1beta"))
  }

  test("ApiVersion encoder should encode V1 correctly") {
    val version: ApiVersion.Version[ApiVersion.V1Capabilities] = ApiVersion.V1
    val encoded                                                = version.asJson
    assertEquals(encoded.asString, Some("v1"))
  }

  test("ApiVersion decoder should decode v1beta") {
    val result = decode[ApiVersion.Version[?]]("\"v1beta\"")
    assertEquals(result, Right(ApiVersion.V1Beta))
  }

  test("ApiVersion decoder should decode v1") {
    val result = decode[ApiVersion.Version[?]]("\"v1\"")
    assertEquals(result, Right(ApiVersion.V1))
  }

  test("ApiVersion decoder should fail for unknown version") {
    val result = decode[ApiVersion.Version[?]]("\"v2\"")
    assert(result.isLeft)
  }

  test("GeminiConfig should use v1beta by default") {
    val config = GeminiConfig("test-key")
    assertEquals(config.apiVersion, ApiVersion.V1Beta)
    assertEquals(config.versionPath, "v1beta")
  }

  test("GeminiConfig.v1beta should create v1beta config") {
    val config = GeminiConfig.v1beta("test-key")
    assertEquals(config.versionedBaseUrl, "https://generativelanguage.googleapis.com/v1beta")
  }

  test("GeminiConfig.v1 should create v1 config") {
    val config = GeminiConfig.v1("test-key")
    assertEquals(config.versionedBaseUrl, "https://generativelanguage.googleapis.com/v1")
  }

  test("v1betaOnlyResources should contain expected resources") {
    assert(ApiVersion.v1betaOnlyResources.contains("cachedContents"))
    assert(ApiVersion.v1betaOnlyResources.contains("files"))
    assert(ApiVersion.v1betaOnlyResources.contains("media"))
  }

  test("v1OnlyResources should contain operations") {
    assert(ApiVersion.v1OnlyResources.contains("operations"))
  }
}
