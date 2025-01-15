# gemini4s

A Tagless Final Scala library for the Google Gemini API built on top of ZIO.

[![Scala CI](https://github.com/JamesMMiller/gemini4s/actions/workflows/scala.yml/badge.svg?branch=main)](https://github.com/JamesMMiller/gemini4s/actions/workflows/scala.yml)

## Features

- Tagless Final design for maximum flexibility and composability
- ZIO-based implementation for excellent concurrency and resource management
- Type-safe API interactions
- Streaming support for content generation
- Comprehensive error handling
- Easy integration with existing ZIO applications

## Getting Started

### Prerequisites

- Scala 3.3.1
- SBT
- JDK 11 or higher
- Google Cloud API key for Gemini

### Installation

Add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "io.github.jamesmiller" %% "gemini4s" % "<version>"
```

### Basic Usage

Here's a simple example of using gemini4s to generate content:

```scala
import zio._
import gemini4s._
import gemini4s.config.GeminiConfig
import gemini4s.model.GeminiRequest._

object Example extends ZIOAppDefault {
  def run = {
    val program = for {
      service <- ZIO.service[GeminiService[Task]]
      response <- service.generateContent(
        contents = List(Content(parts = List(Part(text = "Tell me a joke about programming"))))
      )(using GeminiConfig("your-api-key"))
      _ <- response match {
        case Right(result) => Console.printLine(result.candidates.head.content.parts.head.text)
        case Left(error) => Console.printLine(s"Error: ${error.message}")
      }
    } yield ()

    program.provide(
      GeminiLive.layer
    )
  }
}
```

### Advanced Features

#### Streaming Content Generation

For long-form content, you can use streaming to get responses in real-time:

```scala
def streamContent(prompt: String)(using config: GeminiConfig) = {
  for {
    service <- ZIO.service[GeminiService[Task]]
    stream <- service.generateContentStream(
      contents = List(Content(parts = List(Part(text = prompt))))
    )
    _ <- stream
      .map(_.candidates.head.content.parts.head.text)
      .tap(Console.printLine(_))
      .runDrain
  } yield ()
}
```

#### Error Handling

The library provides comprehensive error handling with retry and fallback strategies:

```scala
val program = for {
  service <- ZIO.service[GeminiService[Task]]
  result <- service.generateContent(
    contents = List(Content(parts = List(Part(text = prompt))))
  )(using config)
    .tapError(error => Console.printLine(s"Error occurred: ${error.message}"))
    .retry(Schedule.exponential(1.second) && Schedule.recurs(3))
    .catchAll {
      case GeminiError.RateLimitError(_) => 
        Console.printLine("Rate limit reached, waiting...") *>
        ZIO.sleep(5.seconds) *>
        service.generateContent(contents)(using config)
      case GeminiError.NetworkError(_, _) =>
        Console.printLine("Network error, using fallback...") *>
        ZIO.succeed(Right(fallbackResponse))
      case _ => 
        Console.printLine("Unrecoverable error, terminating") *>
        ZIO.fail(error)
    }
} yield result
```

#### Combining Features

You can combine multiple features like streaming, safety checks, and token counting:

```scala
val program = for {
  service <- ZIO.service[GeminiService[Task]]
  
  // First check token count
  tokenCount <- service.countTokens(
    List(Content(parts = List(Part(text = prompt))))
  )(using config)
  _ <- tokenCount match {
    case Right(count) => Console.printLine(s"Token count: $count")
    case Left(error) => Console.printLine(s"Token count error: ${error.message}")
  }
  
  // Then stream content with safety and generation settings
  stream <- service.generateContentStream(
    contents = List(Content(parts = List(Part(text = prompt)))),
    safetySettings = Some(safetySettings),
    generationConfig = Some(generationConfig)
  )(using config)
  
  _ <- stream
    .tap { response =>
      // Log safety ratings
      val safetyInfo = response.candidates.head.safetyRatings
        .map(rating => s"${rating.category}: ${rating.probability}")
        .mkString("\n")
      Console.printLine(s"Safety Ratings:\n$safetyInfo")
    }
    .map(_.candidates.head.content.parts.head.text)
    .tap(Console.printLine(_))
    .runDrain
    .catchAll { error =>
      Console.printLine(s"Stream error: ${error.message}") *>
      ZIO.unit
    }
} yield ()
```

#### Safety Settings

Control content safety with customizable settings:

```scala
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
  contents = List(Content(parts = List(Part(text = prompt)))),
  safetySettings = Some(safetySettings)
)
```

#### Generation Configuration

Fine-tune the generation parameters:

```scala
val config = GenerationConfig(
  temperature = Some(0.8),
  topK = Some(10),
  topP = Some(0.9),
  candidateCount = Some(1),
  maxOutputTokens = Some(2048)
)

service.generateContent(
  contents = List(Content(parts = List(Part(text = prompt)))),
  generationConfig = Some(config)
)
```

### Example CLI Application

The project includes a full-featured CLI example that demonstrates all major features. Run it with:

```bash
sbt "examples/run generate <api-key> 'your prompt' --temperature=0.8 --max-tokens=2048 --safety=true"
```

Available commands:
- `generate`: Generate content from a prompt
- `stream`: Stream content from a prompt
- `count`: Count tokens in text

Options:
- `--temperature=<value>`: Set temperature (0.0-1.0, default 0.8)
- `--max-tokens=<value>`: Set max output tokens (default 2048)
- `--safety=<true/false>`: Enable/disable safety settings (default false)
- `--model=<model>`: Set model name (default gemini-pro)

## Project Structure

The project is organized into distinct layers:

### Foundation Layer
- Error Types: Comprehensive ADTs for all error cases
- Core Models: Request/response models with JSON codecs

### Core API Layer
- Core Algebra: Tagless final traits defining the API
- HTTP Client: ZIO-based HTTP implementation

### Implementation Layer
- ZIO Interpreter: Complete implementation of the algebra
- Streaming Support: Real-time content generation

### Documentation Layer
- Examples: Sample projects and use cases
- API Documentation: Comprehensive guides and references

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details on:
- Development process
- Code style guidelines
- Testing requirements
- PR process
- Documentation requirements

Check our [Project Board](https://github.com/users/JamesMMiller/projects/3) for available tasks and current progress.

## License

This project is licensed under the Apache 2.0 License - see the LICENSE file for details. 
