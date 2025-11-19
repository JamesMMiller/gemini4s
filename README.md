# gemini4s

A Tagless Final Scala library for the Google Gemini API, built on the Typelevel stack (Cats Effect, FS2, Sttp, Circe).

[![Scala CI](https://github.com/JamesMMiller/gemini4s/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/JamesMMiller/gemini4s/actions/workflows/ci.yml)

## Features

- **Tagless Final Design**: Maximum flexibility and composability with `F[_]`.
- **Typelevel Stack**: Built with Cats Effect 3, FS2, Sttp 3, and Circe.
- **Type-safe API**: Strongly typed request and response models.
- **Streaming Support**: Native `fs2.Stream` support for real-time content generation.
- **Comprehensive Error Handling**: Typed error hierarchy.

## Getting Started

### Prerequisites

- Scala 3.6.2
- JDK 11 or higher
- Google Cloud API key for Gemini

### Installation

Add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "io.github.jamesmiller" %% "gemini4s" % "0.1.0"
```

### Basic Usage

Here's a simple example using `IOApp`:

```scala
import cats.effect.{IO, IOApp}
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import gemini4s.interpreter.GeminiServiceImpl
import gemini4s.http.GeminiHttpClient
import gemini4s.config.GeminiConfig
import gemini4s.model.GeminiService

object Example extends IOApp.Simple {
  val run: IO[Unit] = HttpClientFs2Backend.resource[IO]().use { backend =>
    implicit val config: GeminiConfig = GeminiConfig("YOUR_API_KEY")
    val httpClient = GeminiHttpClient.make[IO](backend)
      _ <- response match {
        case Right(result) => 
          IO.println(result.candidates.head.content.parts.head.text)
        case Left(error) => 
          IO.println(s"Error: ${error.message}")
      }
    } yield ()
  }
}
```

### Advanced Features

#### Streaming Content Generation

Stream responses in real-time using `fs2.Stream`:

```scala
import fs2.Stream

def streamContent(prompt: String)(using config: GeminiConfig, service: GeminiService[IO]): IO[Unit] = {
  val stream = service.generateContentStream(
    contents = List(Content(parts = List(Part(text = prompt))))
  )
  
  stream
    .map(_.candidates.head.content.parts.head.text)
    .evalMap(text => IO.print(text)) // Print chunks as they arrive
    .compile
    .drain
}
```

#### Safety Settings

Control content safety with customizable settings:

```scala
val safetySettings = List(
  SafetySetting(
    category = HarmCategory.HARASSMENT,
    threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
  )
)

service.generateContent(
  contents = List(Content(parts = List(Part(text = "prompt")))),
  safetySettings = Some(safetySettings)
)
```

#### Generation Configuration

Fine-tune generation parameters:

```scala
val genConfig = GenerationConfig(
  temperature = Some(0.8f),
  topK = Some(10),
  candidateCount = Some(1),
  maxOutputTokens = Some(2048)
)

service.generateContent(
  contents = List(Content(parts = List(Part(text = "prompt")))),
  generationConfig = Some(genConfig)
)
```

#### Tool Use (Function Calling)

Define and use tools within your requests:

```scala
import gemini4s.model.GeminiRequest._

val weatherTool = Tool(
  functionDeclarations = Some(List(
    FunctionDeclaration(
      name = "get_weather",
      description = "Get the current weather in a given location",
      parameters = Some(Schema(
        `type` = SchemaType.OBJECT,
        properties = Some(Map(
          "location" -> Schema(`type` = SchemaType.STRING, description = Some("The city and state, e.g. San Francisco, CA"))
        )),
        required = Some(List("location"))
      ))
    )
  ))
)

service.generateContent(
  contents = List(Content(parts = List(Part(text = "What is the weather in London?")))),
  tools = Some(List(weatherTool)),
  toolConfig = Some(ToolConfig(functionCallingConfig = Some(FunctionCallingConfig(mode = Some(FunctionCallingMode.AUTO)))))
)
```

#### JSON Mode

Force the model to output valid JSON:

```scala
val jsonConfig = GenerationConfig(
  responseMimeType = Some("application/json")
)

service.generateContent(
  contents = List(Content(parts = List(Part(text = "List 5 popular fruits in JSON format")))),
  generationConfig = Some(jsonConfig)
)
```

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md).

## License

This project is licensed under the Apache 2.0 License - see the LICENSE file for details.
