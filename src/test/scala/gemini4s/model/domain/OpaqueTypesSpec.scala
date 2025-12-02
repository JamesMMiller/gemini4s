package gemini4s.model.domain

import munit.FunSuite

class OpaqueTypesSpec extends FunSuite {

  test("Temperature validation") {
    assert(Temperature(0.0f).isRight)
    assert(Temperature(1.0f).isRight)
    assert(Temperature(2.0f).isRight)
    assert(Temperature(-0.1f).isLeft)
    assert(Temperature(2.1f).isLeft)
  }

  test("TopK validation") {
    assert(TopK(1).isRight)
    assert(TopK(40).isRight)
    assert(TopK(0).isLeft)
    assert(TopK(-1).isLeft)
  }

  test("TopP validation") {
    assert(TopP(0.1f).isRight)
    assert(TopP(1.0f).isRight)
    assert(TopP(0.0f).isLeft) // Exclusive lower bound
    assert(TopP(-0.1f).isLeft)
    assert(TopP(1.1f).isLeft)
  }

  test("MimeType validation") {
    assert(MimeType("text/plain").isRight)
    assert(MimeType("application/json").isRight)
    assert(MimeType("invalid").isLeft)
    assert(MimeType("").isLeft)
  }
}
