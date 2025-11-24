package gemini4s.model.response

import io.circe.parser._
import munit.FunSuite

class CountTokensResponseSpec extends FunSuite {

  test("CountTokensResponse should decode with only totalTokens") {
    val json   = """{"totalTokens":100}"""
    val result = decode[CountTokensResponse](json)
    assert(result.isRight)
    assertEquals(result.map(_.totalTokens), Right(100))
  }

  test("CountTokensResponse should handle optional candidatesTokenCount") {
    val json   = """{"totalTokens":100,"candidatesTokenCount":50}"""
    val result = decode[CountTokensResponse](json)
    assert(result.isRight)
    assertEquals(result.map(_.candidatesTokenCount), Right(Some(50)))
  }

  test("CountTokensResponse should handle optional cachedContentTokenCount") {
    val json   = """{"totalTokens":100,"cachedContentTokenCount":20}"""
    val result = decode[CountTokensResponse](json)
    assert(result.isRight)
    assertEquals(result.map(_.cachedContentTokenCount), Right(Some(20)))
  }

  test("CountTokensResponse should handle all fields") {
    val json   = """{"totalTokens":100,"candidatesTokenCount":50,"cachedContentTokenCount":20}"""
    val result = decode[CountTokensResponse](json)
    assert(result.isRight)
    result match {
      case Right(response) =>
        assertEquals(response.totalTokens, 100)
        assertEquals(response.candidatesTokenCount, Some(50))
        assertEquals(response.cachedContentTokenCount, Some(20))
      case Left(_)         => fail("Should decode successfully")
    }
  }

  test("CountTokensResponse should handle missing optional fields gracefully") {
    val response = CountTokensResponse(100, None, None)
    assertEquals(response.totalTokens, 100)
    assertEquals(response.candidatesTokenCount, None)
    assertEquals(response.cachedContentTokenCount, None)
  }
}
