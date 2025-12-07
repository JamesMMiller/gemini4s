# Quick Start

This guide will help you get started with gemini4s in just a few minutes.

## Prerequisites

- Scala 3.6.2 or higher
- JDK 11 or higher
- A Google Gemini API key ([Get one here](https://makersuite.google.com/app/apikey))

## Installation

Add gemini4s to your `build.sbt`:

```scala
libraryDependencies += "io.github.jamesmmiller" %% "gemini4s" % "@VERSION@"
```

## Getting an API Key

1. Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Click "Create API Key"
3. Copy your API key
4. Store it securely (never commit it to version control!)

## Basic Setup

Here's the minimal setup to start using gemini4s:

```scala mdoc:compile-only
import cats.effect.{IO, IOApp}
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.domain.ModelName

object QuickStart extends IOApp.Simple {

  val run: IO[Unit] = {
    val apiKey = sys.env.getOrElse("GEMINI_API_KEY", "your-api-key")
    val config = GeminiConfig(apiKey)

    GeminiService.make[IO](config).use { service =>
      service.generateContent(
        GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Explain quantum computing")))
      ).flatMap {
        case Right(response) => 
          IO.println(response.candidates.head.content.flatMap(_.parts.headOption).getOrElse("No content"))
        case Left(error) => 
          IO.println(s"Error: ${error.message}")
      }
    }
  }
}
```

## Choosing an API Version

By default, gemini4s gives you the full v1beta service with all capabilities. For a minimal service, use `makeV1`:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig

// Full v1beta service (default) - all capabilities
GeminiService.make[IO](GeminiConfig.v1beta("key")).use { svc =>
  // svc has: GeminiCore & GeminiFiles & GeminiCaching & GeminiBatch
  IO.unit
}

// Minimal v1 service - core capabilities only
GeminiService.makeV1[IO]("key").use { svc =>
  // svc has: GeminiCore only
  // File/caching/batch methods won't compile!
  IO.unit
}
```

See [API Compliance](api-compliance.md) for details on feature differences between versions.

## Next Steps

Now that you have the basics, explore:

- **[Core Concepts](core-concepts.md)** - Understand the library's design
- **[File API](files.md)** - Upload files for multimodal generation
- **[Examples](examples.md)** - See complete working examples
- **[Best Practices](best-practices.md)** - Production-ready patterns

## Troubleshooting

### "Invalid API Key" Error

Make sure your API key is correct and has the necessary permissions. Test it in [Google AI Studio](https://makersuite.google.com/app/apikey) first.

### Connection Timeout

The default timeout is 60 seconds. For longer requests, you may need to adjust the backend configuration.

### Rate Limiting

Free tier has rate limits. See the [Gemini API documentation](https://ai.google.dev/pricing) for details on quotas.
