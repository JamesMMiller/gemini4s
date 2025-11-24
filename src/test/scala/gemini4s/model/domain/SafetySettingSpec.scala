package gemini4s.model.domain

import munit.FunSuite

class SafetySettingSpec extends FunSuite {

  test("SafetySetting should create with category and threshold") {
    val setting = SafetySetting(
      category = HarmCategory.HARASSMENT,
      threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
    )
    assertEquals(setting.category, HarmCategory.HARASSMENT)
    assertEquals(setting.threshold, HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE)
  }

  test("SafetySetting should handle all harm categories") {
    val categories = List(
      HarmCategory.HARASSMENT,
      HarmCategory.HATE_SPEECH,
      HarmCategory.SEXUALLY_EXPLICIT,
      HarmCategory.DANGEROUS_CONTENT
    )

    categories.foreach { category =>
      val setting = SafetySetting(category, HarmBlockThreshold.BLOCK_NONE)
      assertEquals(setting.category, category)
    }
  }

  test("SafetySetting should handle all thresholds") {
    val thresholds = List(
      HarmBlockThreshold.BLOCK_NONE,
      HarmBlockThreshold.BLOCK_LOW_AND_ABOVE,
      HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
      HarmBlockThreshold.BLOCK_ONLY_HIGH
    )

    thresholds.foreach { threshold =>
      val setting = SafetySetting(HarmCategory.HARASSMENT, threshold)
      assertEquals(setting.threshold, threshold)
    }
  }
}
