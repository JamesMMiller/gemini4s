# gemini4s

A Tagless Final Scala library for the Google Gemini API built on top of ZIO.

[![Scala CI](https://github.com/JamesMMiller/gemini4s/actions/workflows/scala.yml/badge.svg?branch=main)](https://github.com/JamesMMiller/gemini4s/actions/workflows/scala.yml)

## Features

- Tagless Final design for maximum flexibility and composability
- ZIO-based implementation for excellent concurrency and resource management
- Type-safe API interactions
- Streaming support for content generation
- Robust error handling (see docs/API_DOCUMENTATION.md for details)
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

## Documentation

For a detailed API reference—including module overviews, configuration options, error-handling strategies, and usage examples—please see our [Comprehensive API Documentation](docs/API_DOCUMENTATION.md).

## Basic Usage

Here's a simple example of using gemini4s to generate content:

```scala
import zio._
import gemini4s._
import gemini4s.config.GeminiConfig
import gemini4s.model.GeminiRequest._

object Example extends ZIOAppDefault {
  def run = {
    val config = GeminiConfig("your-api-key")
    val program = for {
      service  <- ZIO.service[GeminiService[Task]]
      response <- service.generateContent(
                    contents = List(Content(parts = List(Part(text = "Tell me a joke about programming"))))
                  )(using config)
      _ <- response match {
            case Right(result) => Console.printLine(result.candidates.head.content.parts.head.text)
            case Left(error)   => Console.printLine(s"Error: ${error.message}")
          }
    } yield ()

    program.provide(GeminiLive.layer)
  }
}
```

## CLI Application

The repository includes a CLI application for interacting with the Gemini API.

### Building the CLI

```bash
sbt "examples/assembly"
```

### Running the CLI

```bash
./gemini4s-cli generate <api-key> "Your prompt here" [options]
```

Options include:

- `--temperature=<value>`
- `--max-tokens=<value>`
- `--safety=<true/false>`
- `--model=<model>`

## Contributing & Code Quality

We welcome contributions! Please review our [Contributing Guide](CONTRIBUTING.md) and follow the [Cursor Rules](.cursorrules) to learn more about our development process, testing requirements, and PR guidelines.

## Project Board

Stay up to date with available tasks and current progress on our [Project Board](https://github.com/users/JamesMMiller/projects/3).

## License

This project is licensed under the Apache 2.0 License - see the LICENSE file for details.
