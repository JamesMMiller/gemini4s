# gemini4s

**A Tagless Final Scala library for the Google Gemini API**

gemini4s is a purely functional, type-safe Scala 3 library for interacting with Google's Gemini API. Built on the Typelevel stack (Cats Effect, FS2, Sttp, Circe), it provides a composable and effect-polymorphic interface for content generation, streaming, embeddings, and more.

## Why gemini4s?

### Tagless Final Design
gemini4s follows the Tagless Final pattern, allowing you to abstract over effect types (`F[_]`). This means you can use it with any effect type that has an `Async` instance - typically Cats Effect's `IO`, but also ZIO, Monix, or custom effect types.

### Type-Safe API
All requests and responses are strongly typed with case classes and enums, catching errors at compile time rather than runtime. The API models closely follow the official Gemini API specification.

### Streaming Support
Native FS2 streaming support for real-time content generation. Process responses as they arrive without waiting for the complete response.

### Purely Functional
Built on Cats Effect 3 for composable, referentially transparent effects. All side effects are properly tracked in the type system.

### Comprehensive Error Handling
Typed error hierarchy with specific error types for different failure modes (authentication, rate limiting, content safety, network issues, etc.).

## Quick Start

### Installation

Add the following to your `build.sbt`:

```scala
libraryDependencies += "io.github.jamesmmiller" %% "gemini4s" % "@VERSION@"
```

### Your First API Call

```scala mdoc:compile-only
import cats.effect.{IO, IOApp}
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import gemini4s.GeminiService
import gemini4s.interpreter.GeminiServiceImpl
import gemini4s.http.GeminiHttpClient
import gemini4s.config.GeminiConfig

object QuickStart extends IOApp.Simple {
  val run: IO[Unit] = HttpClientFs2Backend.resource[IO]().use { backend =>
    given GeminiConfig = GeminiConfig("YOUR_API_KEY")
    
    val httpClient = GeminiHttpClient.make[IO](backend)
    val service = GeminiServiceImpl.make[IO](httpClient)
    
    for {
      response <- service.generateContent(
        contents = List(GeminiService.text("Explain quantum computing in one sentence"))
      )
      _ <- response match {
        case Right(result) => 
          IO.println(result.candidates.head.content.parts.head)
        case Left(error) => 
          IO.println(s"Error: ${error.message}")
      }
    } yield ()
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

## API Reference

See the [API documentation](https://javadoc.io/doc/io.github.jamesmmiller/gemini4s_3) for detailed scaladoc.

## Available Models

gemini4s supports all Gemini models:

- `gemini-2.5-flash` - Fast, versatile performance (default)
- `gemini-2.5-pro` - Complex reasoning tasks
- `gemini-2.5-flash-lite` - Lightweight, high-volume tasks
- `text-embedding-004` - Text embeddings

See the [Models Guide](models.md) for detailed comparison and selection guidance.

## Community

- **GitHub**: [JamesMMiller/gemini4s](https://github.com/JamesMMiller/gemini4s)
- **Issues**: [Report bugs or request features](https://github.com/JamesMMiller/gemini4s/issues)
- **Contributing**: See [CONTRIBUTING.md](https://github.com/JamesMMiller/gemini4s/blob/main/CONTRIBUTING.md)

## License

gemini4s is licensed under the Apache 2.0 License. See [LICENSE](https://github.com/JamesMMiller/gemini4s/blob/main/LICENSE) for details.
