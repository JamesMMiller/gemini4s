# gemini4s

**The Typelevel Scala Client for Google Gemini**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.jamesmmiller/gemini4s_3.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.jamesmmiller/gemini4s_3)
[![CI](https://github.com/JamesMMiller/gemini4s/actions/workflows/ci.yml/badge.svg)](https://github.com/JamesMMiller/gemini4s/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://github.com/JamesMMiller/gemini4s/blob/main/LICENSE)

gemini4s is a **purely functional**, **type-safe**, and **idiomatic** Scala 3 library for the Google Gemini API. Built on the Typelevel stack (Cats Effect, FS2, Sttp, Circe), it brings the power of Gemini to your Scala applications with a developer experience you'll love.

## Key Features

- 🚀 **Tagless Final**: Abstract over effect types (`IO`, `ZIO`, `Task`, etc.)
- 🛡️ **Type-Safe**: Strongly typed models for requests, responses, and configuration
- 🌊 **Streaming**: Native FS2 streaming for real-time responses
- 🧩 **Composable**: Built on Cats Effect 3 for seamless integration
- 🛡️ **Robust**: Comprehensive error handling and retry strategies
- 🛠️ **Feature Complete**: Supports content generation, embeddings, function calling, caching, and more

## Quick Start

### Installation

Add the following to your `build.sbt`:

```scala
libraryDependencies += "io.github.jamesmmiller" %% "gemini4s" % "@VERSION@"
```

### Your First API Call

```scala mdoc:compile-only
import cats.effect.{IO, IOApp}
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.domain.ModelName

object Example extends IOApp.Simple {
  val run: IO[Unit] = {
    val config = GeminiConfig(sys.env.getOrElse("GEMINI_API_KEY", "your-api-key"))
    
    GeminiService.make[IO](config).use { service =>
      service.generateContent(
        GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Explain quantum computing in one sentence")))
      ).flatMap {
        case Right(response) => IO.println(response.candidates.head.content.flatMap(_.parts.headOption).getOrElse("No content"))
        case Left(error)     => IO.println(s"Error: ${error.message}")
      }
    }
  }
}
```

## Features

- **[Content Generation](content-generation.md)** - Generate text with customizable parameters
- **[Streaming](streaming.md)** - Real-time content generation with FS2 streams
- **[Function Calling](function-calling.md)** - Tool use and function calling capabilities
- **[Embeddings](embeddings.md)** - Generate embeddings for semantic search and clustering
- **[Safety Settings](safety.md)** - Configure content filtering and safety thresholds
- **[Context Caching](caching.md)** - Optimize costs with context caching
- **[Error Handling](error-handling.md)** - Comprehensive error types and recovery strategies

## Documentation

- **[Quick Start Guide](quickstart.md)** - Get up and running quickly
- **[Core Concepts](core-concepts.md)** - Understand the library's design
- **[Examples](examples.md)** - Complete working examples
- **[Best Practices](best-practices.md)** - Production-ready patterns
- **[FAQ](faq.md)** - Common questions and troubleshooting
- **[Roadmap](roadmap.md)** - Future development plans

## API Reference

See the [API documentation](https://javadoc.io/doc/io.github.jamesmmiller/gemini4s_3) for detailed scaladoc.

## Available Models

gemini4s supports all Gemini models:

- `gemini-3-pro-preview` - Latest reasoning model
- `gemini-2.5-flash` - Fast, versatile performance
- `gemini-2.5-pro` - Complex reasoning tasks
- `gemini-2.5-flash-lite` - Lightweight, high-volume tasks
- `gemini-embedding-001` - Text embeddings
- `gemini-pro-latest` - Latest stable Pro model
- `gemini-flash-latest` - Latest stable Flash model (default)

See the [Models Guide](models.md) for detailed comparison and selection guidance.

## Community

- **GitHub**: [JamesMMiller/gemini4s](https://github.com/JamesMMiller/gemini4s)
- **Issues**: [Report bugs or request features](https://github.com/JamesMMiller/gemini4s/issues)
- **Contributing**: See [Contributing Guide](contributing.md)

## License

gemini4s is licensed under the Apache 2.0 License. See [LICENSE](https://github.com/JamesMMiller/gemini4s/blob/main/LICENSE) for details.
