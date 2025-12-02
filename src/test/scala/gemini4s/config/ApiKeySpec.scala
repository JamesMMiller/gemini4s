package gemini4s.config

import munit.FunSuite

class ApiKeySpec extends FunSuite {

  test("ApiKey validation") {
    assert(ApiKey("valid-key").isRight)
    assert(ApiKey("").isLeft)
    assert(ApiKey("   ").isLeft)
  }
}
