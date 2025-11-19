# Content Generation

Learn how to generate content with the Gemini API using gemini4s.

## Basic Generation

The simplest way to generate content:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig

def basic(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  service.generateContent(
    contents = List(GeminiService.text("Explain photosynthesis"))
  ).flatMap {
    case Right(response) =>
      val text = response.candidates.head.content.parts.head
      IO.println(text)
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

## Generation Configuration

Control how content is generated with `GenerationConfig`:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.GeminiRequest.GenerationConfig
import gemini4s.config.GeminiConfig

def withConfig(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  val config = GenerationConfig(
    temperature = Some(0.7f),      // Creativity (0.0 - 2.0)
    topK = Some(40),                // Top-k sampling
    topP = Some(0.95f),             // Nucleus sampling
    maxOutputTokens = Some(1024),   // Max response length
    stopSequences = Some(List("\n\n")) // Stop generation at these sequences
  )
  
  service.generateContent(
    contents = List(GeminiService.text("Write a haiku")),
    generationConfig = Some(config)
  ).void
}
```

### Temperature

Controls randomness in the output:
- **0.0**: Deterministic, focused responses
- **1.0**: Balanced creativity and coherence (default)
- **2.0**: Maximum creativity, less predictable

```scala mdoc:compile-only
import gemini4s.model.GeminiRequest.GenerationConfig

// For factual, consistent responses
val factual = GenerationConfig(temperature = Some(0.2f))

// For creative writing
val creative = GenerationConfig(temperature = Some(1.5f))
```

### Top-K and Top-P

Control token selection:

- **topK**: Consider only the K most likely tokens
- **topP**: Consider tokens whose cumulative probability is P

```scala mdoc:compile-only
import gemini4s.model.GeminiRequest.GenerationConfig

// More focused (fewer options)
val focused = GenerationConfig(topK = Some(10), topP = Some(0.8f))

// More diverse (more options)
val diverse = GenerationConfig(topK = Some(100), topP = Some(0.95f))
```

## System Instructions

Provide system-level instructions that guide the model's behavior:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.GeminiRequest.Content
import gemini4s.config.GeminiConfig

def withSystemInstruction(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  val systemInstruction = Content(
    parts = List(gemini4s.model.GeminiRequest.Part(
      "You are a helpful assistant that always responds in a friendly, " +
      "encouraging tone. Keep responses concise."
    ))
  )
  
  service.generateContent(
    contents = List(GeminiService.text("How do I learn Scala?")),
    systemInstruction = Some(systemInstruction)
  ).void
}
```

## Multi-Turn Conversations

Build conversations by providing message history:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.GeminiRequest.{Content, Part}
import gemini4s.config.GeminiConfig

def conversation(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  val history = List(
    Content(parts = List(Part("What is Scala?")), role = Some("user")),
    Content(
      parts = List(Part("Scala is a programming language...")),
      role = Some("model")
    ),
    Content(parts = List(Part("What are its main features?")), role = Some("user"))
  )
  
  service.generateContent(contents = history).void
}
```

## JSON Mode

Force the model to output valid JSON:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.GeminiRequest.GenerationConfig
import gemini4s.config.GeminiConfig

def jsonMode(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  val jsonConfig = GenerationConfig(
    responseMimeType = Some("application/json")
  )
  
  service.generateContent(
    contents = List(GeminiService.text(
      "List 5 programming languages with their year of creation in JSON format"
    )),
    generationConfig = Some(jsonConfig)
  ).flatMap {
    case Right(response) =>
      val json = response.candidates.head.content.parts.head
      IO.println(s"JSON response: $json")
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

## Candidate Count

Request multiple response candidates:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.GeminiRequest.GenerationConfig
import gemini4s.config.GeminiConfig

def multipleCandidates(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  val config = GenerationConfig(
    candidateCount = Some(3)  // Get 3 different responses
  )
  
  service.generateContent(
    contents = List(GeminiService.text("Suggest a name for a cat")),
    generationConfig = Some(config)
  ).flatMap {
    case Right(response) =>
      response.candidates.traverse_ { candidate =>
        IO.println(s"Candidate: ${candidate.content.parts.head}")
      }
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

## Token Counting

Count tokens before making a request:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig

def countTokens(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  val content = GeminiService.text("This is a long prompt...")
  
  service.countTokens(List(content)).flatMap {
    case Right(count) =>
      IO.println(s"Token count: $count")
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

## Response Metadata

Access usage metadata from responses:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig

def checkMetadata(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  service.generateContent(
    contents = List(GeminiService.text("Hello"))
  ).flatMap {
    case Right(response) =>
      response.usageMetadata.traverse_ { metadata =>
        IO.println(s"Prompt tokens: ${metadata.promptTokenCount}") *>
        IO.println(s"Response tokens: ${metadata.candidatesTokenCount}") *>
        IO.println(s"Total tokens: ${metadata.totalTokenCount}")
      }
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

## Best Practices

### 1. Use Appropriate Temperature

- **Factual tasks**: Low temperature (0.0 - 0.3)
- **Creative tasks**: Medium to high temperature (0.7 - 1.5)
- **Code generation**: Low to medium temperature (0.2 - 0.7)

### 2. Set Max Tokens

Always set `maxOutputTokens` to avoid unexpectedly long responses:

```scala mdoc:compile-only
import gemini4s.model.GeminiRequest.GenerationConfig

val config = GenerationConfig(
  maxOutputTokens = Some(512)  // Limit response length
)
```

### 3. Handle Partial Responses

Check the `finishReason` to see if the response was truncated:

```scala mdoc:compile-only
import cats.effect.IO

def checkFinishReason(response: gemini4s.model.GeminiResponse.GenerateContentResponse): IO[Unit] = {
  response.candidates.headOption.flatMap(_.finishReason) match {
    case Some("MAX_TOKENS") =>
      IO.println("Response was truncated due to max tokens")
    case Some("SAFETY") =>
      IO.println("Response was blocked by safety filters")
    case Some("STOP") =>
      IO.println("Response completed normally")
    case other =>
      IO.println(s"Finish reason: $other")
  }
}
```

### 4. Reuse Configuration

Create reusable configurations for common use cases:

```scala mdoc:compile-only
import gemini4s.model.GeminiRequest.GenerationConfig

object Configs {
  val factual = GenerationConfig(
    temperature = Some(0.2f),
    topP = Some(0.8f),
    maxOutputTokens = Some(1024)
  )
  
  val creative = GenerationConfig(
    temperature = Some(1.2f),
    topP = Some(0.95f),
    maxOutputTokens = Some(2048)
  )
  
  val concise = GenerationConfig(
    temperature = Some(0.7f),
    maxOutputTokens = Some(256)
  )
}
```

## Next Steps

- **[Streaming](streaming.md)** - Stream responses in real-time
- **[Function Calling](function-calling.md)** - Use tools and functions
- **[Safety Settings](safety.md)** - Configure content filtering
- **[Examples](examples.md)** - See complete working examples
