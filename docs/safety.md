# Safety Settings

Configure content filtering and safety thresholds to control generated content.

## Harm Categories

Gemini filters content across these categories:

```scala mdoc:compile-only
import gemini4s.model.domain.HarmCategory

val categories = List(
  HarmCategory.HARASSMENT,
  HarmCategory.HATE_SPEECH,
  HarmCategory.SEXUALLY_EXPLICIT,
  HarmCategory.DANGEROUS_CONTENT
)
```

## Block Thresholds

Control how aggressively content is filtered:

```scala mdoc:compile-only
import gemini4s.model.domain.HarmBlockThreshold

// Most permissive - only block high-probability harmful content
val blockOnlyHigh = HarmBlockThreshold.BLOCK_ONLY_HIGH

// Moderate - block medium and high probability (recommended)
val blockMediumAndAbove = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE

// Strict - block low, medium, and high probability
val blockLowAndAbove = HarmBlockThreshold.BLOCK_LOW_AND_ABOVE

// No filtering (use with caution)
val blockNone = HarmBlockThreshold.BLOCK_NONE
```

## Basic Safety Configuration

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain.{SafetySetting, HarmCategory, HarmBlockThreshold, GeminiConstants}
import gemini4s.model.request.GenerateContentRequest
import gemini4s.config.GeminiConfig

def withSafety(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  val safetySettings = List(
    SafetySetting(
      category = HarmCategory.HARASSMENT,
      threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
    ),
    SafetySetting(
      category = HarmCategory.HATE_SPEECH,
      threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
    )
  )
  
  service.generateContent(
    GenerateContentRequest(
      GeminiConstants.DefaultModel,
      List(GeminiService.text("Your prompt here")),
      safetySettings = Some(safetySettings)
    )
  ).void
}
```

## Handling Blocked Content

Check if content was blocked:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.model.response.GenerateContentResponse

def checkBlocked(response: GenerateContentResponse): IO[Unit] = {
  response.promptFeedback.flatMap(_.blockReason) match {
    case Some(reason) =>
      IO.println(s"Content was blocked: $reason")
    case None =>
      IO.println("Content was not blocked")
  }
}
```

## Safety Ratings

Inspect safety ratings in responses:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.model.response.GenerateContentResponse

def inspectSafety(response: GenerateContentResponse): IO[Unit] = {
  response.candidates.headOption.flatMap(_.safetyRatings) match {
    case Some(ratings) =>
      ratings.foreach { rating =>
        println(s"Category: ${rating.category}, Probability: ${rating.probability}")
      }
      IO.unit
    case None =>
      IO.println("No safety ratings available")
  }
}
```

## Recommended Settings

### Production Applications

```scala mdoc:compile-only
import gemini4s.model.domain.{SafetySetting, HarmCategory, HarmBlockThreshold}

val productionSafety = List(
  SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE),
  SafetySetting(HarmCategory.HATE_SPEECH, HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE),
  SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE),
  SafetySetting(HarmCategory.DANGEROUS_CONTENT, HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE)
)
```

### Research/Development

```scala mdoc:compile-only
import gemini4s.model.domain.{SafetySetting, HarmCategory, HarmBlockThreshold}

val developmentSafety = List(
  SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.BLOCK_ONLY_HIGH),
  SafetySetting(HarmCategory.HATE_SPEECH, HarmBlockThreshold.BLOCK_ONLY_HIGH),
  SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, HarmBlockThreshold.BLOCK_ONLY_HIGH),
  SafetySetting(HarmCategory.DANGEROUS_CONTENT, HarmBlockThreshold.BLOCK_ONLY_HIGH)
)
```

## Best Practices

1. **Always set safety settings** in production applications
2. **Log blocked content** for monitoring and improvement
3. **Provide user feedback** when content is blocked
4. **Test with various inputs** to understand filtering behavior
5. **Review safety ratings** to fine-tune thresholds

## Next Steps

- **[Error Handling](error-handling.md)** - Handle safety errors
- **[Best Practices](best-practices.md)** - Production patterns
