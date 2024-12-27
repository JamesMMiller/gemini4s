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

## Development Process

### Documentation and Workflow Changes

All process and workflow changes must be properly documented:
- Update both `.cursorrules` and `README.md` when changing workflows
- Changes to development rules require PR review
- Keep documentation in sync with project board structure
- Process changes should be reflected in CI/CD pipeline when applicable

### Project Layers and Dependencies

Development follows a structured approach with clear dependencies:
1. Foundation Layer (Error Types, Models)
2. Core API Layer (Algebra, HTTP Client) - depends on Foundation
3. Implementation Layer (ZIO Interpreter) - depends on Core API
4. Documentation Layer - depends on Implementation

### CI/CD Pipeline

All changes must pass through our CI/CD pipeline:
- Automated build and test on every PR
- Scoverage checks (minimum 90% coverage)
- Branch protection rules enforced
- PR reviews required before merge
- No direct commits to main branch allowed

Status checks must pass before merge:
- ✅ Build and Test
- ✅ Coverage Check
- ✅ PR Review

### Git Flow

We follow a modified git flow process:
- `main` branch is protected and requires PR review
- Feature branches: `feature/[issue-number]-short-description`
- Bug fixes: `fix/[issue-number]-short-description`
- Releases: `release/v[version]`

All commits should reference issue numbers: `#123: Add feature X`

### Project Board

Development is tracked using our GitHub project board:
1. New work starts in "To Do"
2. Active development in "In Progress"
3. Code review in "Review"
4. Merged and tested work in "Done"

Dependencies between tasks are tracked in issue descriptions and must be respected.

### Testing

We maintain high code quality standards:
- Minimum 90% test coverage (enforced by scoverage)
- Property-based testing with ZIO Test
- Integration tests for external API interactions
- Full test suite must pass before PR merge

To run tests with coverage locally:
```bash
sbt clean coverage test coverageReport
```

Coverage reports will be available in `target/scala-3.3.1/scoverage-report/`.

## Contributing

Contributions are welcome! Please follow these steps:

1. Check the project board for available tasks
2. Verify all task dependencies are met
3. Create a new branch following our naming convention
4. Implement changes with tests (90% coverage required)
5. Submit a PR and link it to the relevant project card
6. Ensure all CI checks pass
7. Respond to review feedback
8. Squash commits when ready to merge

See our [.cursorrules](.cursorrules) file for detailed development guidelines.

## License

This project is licensed under the Apache 2.0 License - see the LICENSE file for details. 
