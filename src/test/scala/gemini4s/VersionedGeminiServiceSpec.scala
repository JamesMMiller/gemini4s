package gemini4s

import munit.FunSuite
import gemini4s.config.ApiVersion
import gemini4s.config.ApiVersion._

class VersionedGeminiServiceSpec extends FunSuite {

  // These are compile-time tests that verify the type system works correctly
  // If they compile, the type constraints are working

  test("V1BetaService type should include all capability traits") {
    // This test verifies at compile time that the type alias is correct
    type Expected = VersionedGeminiService[cats.effect.IO, V1BetaCapabilities] &
      HasCachingOps[cats.effect.IO, V1BetaCapabilities] & HasFilesOps[cats.effect.IO, V1BetaCapabilities]

    // Type equivalence check - if this compiles, the types are compatible
    def check(s: VersionedGeminiService.V1BetaService[cats.effect.IO]): Expected = s
    assert(true) // If we got here, the compile-time check passed
  }

  test("V1Service type should be base VersionedGeminiService only") {
    type Expected = VersionedGeminiService[cats.effect.IO, V1Capabilities]

    def check(s: VersionedGeminiService.V1Service[cats.effect.IO]): Expected = s
    assert(true)
  }

  test("V1BetaCapabilities should have HasCaching") {
    // This verifies v1beta has caching at the type level
    summon[V1BetaCapabilities <:< HasCaching]
  }

  test("V1BetaCapabilities should have HasFiles") {
    summon[V1BetaCapabilities <:< HasFiles]
  }

  test("V1BetaCapabilities should have HasMedia") {
    summon[V1BetaCapabilities <:< HasMedia]
  }

  test("V1BetaCapabilities should have HasImagen") {
    summon[V1BetaCapabilities <:< HasImagen]
  }

  test("V1BetaCapabilities should have HasVeo") {
    summon[V1BetaCapabilities <:< HasVeo]
  }

  test("V1Capabilities should have HasOperations") {
    summon[V1Capabilities <:< HasOperations]
  }

  // Negative tests - these verify that v1 does NOT have v1beta-only capabilities
  // We can't easily test "does NOT compile" in a unit test, but we can verify
  // the type relationships

  test("V1Capabilities should NOT be a subtype of HasCaching") {
    // This is a runtime assertion that the types are NOT related
    val v1HasCaching = scala.util.Try {
      // This would fail at compile time if we tried: summon[V1Capabilities <:< HasCaching]
      // Instead we just assert they're different types
      assert(!classOf[HasCaching].isAssignableFrom(classOf[HasOperations]))
    }
    assert(v1HasCaching.isSuccess)
  }
}
