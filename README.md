# gemini4s

**A Tagless Final Scala library for the Google Gemini API**

[![Scala CI](https://github.com/JamesMMiller/gemini4s/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/JamesMMiller/gemini4s/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.jamesmmiller/gemini4s_3.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.jamesmmiller/gemini4s_3)
[![Javadocs](https://javadoc.io/badge/io.github.jamesmmiller/gemini4s_3.svg)](https://javadoc.io/doc/io.github.jamesmmiller/gemini4s_3)

gemini4s is a purely functional, type-safe Scala 3 library for interacting with Google's Gemini API. Built on the Typelevel stack (Cats Effect, FS2, Sttp, Circe), it provides a composable and effect-polymorphic interface for content generation, streaming, embeddings, and more.

## Why gemini4s?

- **🎯 Type-Safe**: Strongly typed request and response models catch errors at compile time
- **🔄 Tagless Final**: Effect-polymorphic design works with IO, ZIO, Monix, or custom effect types
- **📡 Streaming**: Native FS2 streaming support for real-time content generation
- **🛡️ Comprehensive Error Handling**: Typed error hierarchy with specific error types
- **✨ Purely Functional**: Built on Cats Effect 3 for composable, referentially transparent effects
- **📚 Well-Documented**: Extensive documentation with type-checked examples

## Quick Start

### Installation

Add to your `build.sbt`:

```scala
libraryDependencies += "io.github.jamesmmiller" %% "gemini4s" % "0.1.0"
```

### Your First API Call

```scala
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
    
    service.generateContent(
      contents = List(GeminiService.text("Explain quantum computing in one sentence"))
    ).flatMap {
      case Right(result) => 
        IO.println(result.candidates.head.content.parts.head)
      case Left(error) => 
        IO.println(s"Error: ${error.message}")
    }
  }
}
```

## Features

### Content Generation

Generate text with customizable parameters:

```scala
val config = GenerationConfig(
  temperature = Some(0.7f),
  maxOutputTokens = Some(1024),
  topP = Some(0.95f)
)

service.generateContent(
  contents = List(GeminiService.text("Write a haiku about Scala")),
  generationConfig = Some(config)
)
```

### Streaming

Stream responses in real-time with FS2:

```scala
service.generateContentStream(
  contents = List(GeminiService.text("Count from 1 to 10"))
)
  .evalMap(response => IO.println(response.candidates.head.content.parts.head))
  .compile
  .drain
```

### Function Calling

Extend Gemini's capabilities with custom functions:

```scala
val weatherTool = Tool(
  functionDeclarations = Some(List(
    FunctionDeclaration(
      name = "get_weather",
      description = "Get the current weather in a given location",
      parameters = Some(Schema(
        `type` = SchemaType.OBJECT,
        properties = Some(Map(
          "location" -> Schema(`type` = SchemaType.STRING)
        ))
      ))
    )
  ))
)

service.generateContent(
  contents = List(GeminiService.text("What's the weather in Tokyo?")),
  tools = Some(List(weatherTool))
)
```

### Embeddings

Generate embeddings for semantic search:

```scala
service.embedContent(
  content = GeminiService.text("Scala is a programming language"),
  taskType = Some(TaskType.RETRIEVAL_DOCUMENT)
)
```

## Documentation

📖 **[Full Documentation](https://jamesmmiller.github.io/gemini4s/)** - Comprehensive guides and API reference

### Guides

- **[Quick Start](https://jamesmmiller.github.io/gemini4s/quickstart.html)** - Get up and running quickly
- **[Core Concepts](https://jamesmmiller.github.io/gemini4s/core-concepts.html)** - Understand the library's design
- **[Content Generation](https://jamesmmiller.github.io/gemini4s/content-generation.html)** - Generate text with parameters
- **[Streaming](https://jamesmmiller.github.io/gemini4s/streaming.html)** - Real-time content generation
- **[Function Calling](https://jamesmmiller.github.io/gemini4s/function-calling.html)** - Tool use and function calling
- **[Embeddings](https://jamesmmiller.github.io/gemini4s/embeddings.html)** - Semantic search and clustering
- **[Error Handling](https://jamesmmiller.github.io/gemini4s/error-handling.html)** - Retry strategies and patterns
- **[Best Practices](https://jamesmmiller.github.io/gemini4s/best-practices.html)** - Production-ready patterns
- **[FAQ](https://jamesmmiller.github.io/gemini4s/faq.html)** - Common questions and troubleshooting

## Available Models

- `gemini-2.5-flash` - Fast, versatile performance (default)
- `gemini-2.5-pro` - Complex reasoning tasks
- `gemini-2.5-flash-lite` - Lightweight, high-volume tasks
- `text-embedding-004` - Text embeddings

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

Built with the excellent [Typelevel](https://typelevel.org/) ecosystem:
- [Cats Effect](https://typelevel.org/cats-effect/) - Purely functional effects
- [FS2](https://fs2.io/) - Functional streaming
- [Sttp](https://sttp.softwaremill.com/) - HTTP client
- [Circe](https://circe.github.io/circe/) - JSON library
