package gemini4s.config

import munit.FunSuite

class ApiKeySpec extends FunSuite {

  test("ApiKey validation") {
    assert(ApiKey("valid-key").isRight)
    assert(ApiKey("").isLeft)
    assert(ApiKey("   ").isLeft)
  }

  test("ApiKey.unsafe should return ApiKey for valid strings") {
    assertEquals(ApiKey.unsafe("valid").value, "valid")
  }

  test("ApiKey.unsafe should throw for invalid strings") {
    intercept[IllegalArgumentException] {
      ApiKey.unsafe("")
    }
  }

  test("ApiKey encoder should encode to string") {
    import io.circe.syntax.*
    val key  = ApiKey.unsafe("test-key")
    val json = key.asJson
    assertEquals(json.asString, Some("test-key"))
  }

  test("ApiKey decoder should decode from string") {
    import io.circe.parser.*
    val json    = """"valid-key""""
    val decoded = decode[ApiKey](json)
    assert(decoded.isRight)
    assertEquals(decoded.map(_.value), Right("valid-key"))
  }

  test("ApiKey decoder should fail for empty string") {
    import io.circe.parser.*
    val json    = """""""
    val decoded = decode[ApiKey](json)
    assert(decoded.isLeft)
  }
}
