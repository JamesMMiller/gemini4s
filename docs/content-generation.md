# Content Generation

Learn how to generate content with the Gemini API using gemini4s.

## Basic Generation

The simplest way to generate content:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.domain.GeminiConstants

// Assuming 'service' is available (see Quick Start)
def basic(service: GeminiService[IO]): IO[Unit] = {
  service.generateContent(
    GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Explain photosynthesis")))
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
import gemini4s.model.domain.{GenerationConfig, GeminiConstants}
import gemini4s.model.request.GenerateContentRequest

def withConfig(service: GeminiService[IO]): IO[Unit] = {
  val config = GenerationConfig(
    temperature = Some(0.7f),      // Creativity (0.0 - 2.0)
    topK = Some(40),                // Top-k sampling
    topP = Some(0.95f),             // Nucleus sampling
    maxOutputTokens = Some(1024),   // Max response length
    stopSequences = Some(List("\n\n")) // Stop generation at these sequences
  )
  
  service.generateContent(
    GenerateContentRequest(
      model = ModelName.Gemini25Flash,
      contents = List(GeminiService.text("Write a haiku")),
      generationConfig = Some(config)
    )
  ).void
}
```

### Temperature

Controls randomness in the output:
- **0.0**: Deterministic, focused responses
- **1.0**: Balanced creativity and coherence (default)
- **2.0**: Maximum creativity, less predictable

```scala mdoc:compile-only
import gemini4s.model.domain.GenerationConfig

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
import gemini4s.model.domain.GenerationConfig

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
import gemini4s.model.domain.{Content, ContentPart, GeminiConstants}
import gemini4s.model.request.GenerateContentRequest

def withSystemInstruction(service: GeminiService[IO]): IO[Unit] = {
  val systemInstruction = Content(
    parts = List(ContentPart(
      "You are a helpful assistant that always responds in a friendly, " +
      "encouraging tone. Keep responses concise."
    ))
  )
  
  service.generateContent(
    GenerateContentRequest(
      model = ModelName.Gemini25Flash,
      contents = List(GeminiService.text("How do I learn Scala?")),
      systemInstruction = Some(systemInstruction)
    )
  ).void
}
```

## Multi-Turn Conversations

Build conversations by providing message history:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain.{Content, ContentPart, GeminiConstants}
import gemini4s.model.request.GenerateContentRequest

def conversation(service: GeminiService[IO]): IO[Unit] = {
  val history = List(
    Content(parts = List(ContentPart("What is Scala?")), role = Some("user")),
    Content(
      parts = List(ContentPart("Scala is a programming language...")),
      role = Some("model")
    ),
    Content(parts = List(ContentPart("What are its main features?")), role = Some("user"))
  )
  
  service.generateContent(
    GenerateContentRequest(ModelName.Gemini25Flash, history)
  ).void
}
```

## JSON Mode

Force the model to output valid JSON:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain.{GenerationConfig, GeminiConstants}
import gemini4s.model.request.GenerateContentRequest

def jsonMode(service: GeminiService[IO]): IO[Unit] = {
  val jsonConfig = GenerationConfig(
    responseMimeType = Some("application/json")
  )
  
  service.generateContent(
    GenerateContentRequest(
      model = ModelName.Gemini25Flash,
      contents = List(GeminiService.text(
        "List 5 programming languages with their year of creation in JSON format"
      )),
      generationConfig = Some(jsonConfig)
    )
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
import gemini4s.model.domain.{GenerationConfig, GeminiConstants}
import gemini4s.model.request.GenerateContentRequest

def multipleCandidates(service: GeminiService[IO]): IO[Unit] = {
  val config = GenerationConfig(
    candidateCount = Some(3)  // Get 3 different responses
  )
  
  service.generateContent(
    GenerateContentRequest(
      model = ModelName.Gemini25Flash,
      contents = List(GeminiService.text("Suggest a name for a cat")),
      generationConfig = Some(config)
    )
  ).flatMap {
    case Right(response) =>
      response.candidates.foreach { candidate =>
        println(s"Candidate: ${candidate.content.parts.head}")
      }
      IO.unit
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
import gemini4s.model.request.CountTokensRequest
import gemini4s.model.domain.GeminiConstants

def countTokens(service: GeminiService[IO]): IO[Unit] = {
  val content = GeminiService.text("This is a long prompt...")
  
  service.countTokens(
    CountTokensRequest(ModelName.Gemini25Flash, List(content))
  ).flatMap {
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
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.domain.GeminiConstants

def checkMetadata(service: GeminiService[IO]): IO[Unit] = {
  service.generateContent(
    GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Hello")))
  ).flatMap {
    case Right(response) =>
      response.usageMetadata match {
        case Some(metadata) =>
          IO.println(s"Prompt tokens: ${metadata.promptTokenCount}") *>
          IO.println(s"Response tokens: ${metadata.candidatesTokenCount}") *>
          IO.println(s"Total tokens: ${metadata.totalTokenCount}")
        case None => IO.unit
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
import gemini4s.model.domain.GenerationConfig

val config = GenerationConfig(
  maxOutputTokens = Some(512)  // Limit response length
)
```

### 3. Handle Partial Responses

Check the `finishReason` to see if the response was truncated:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.model.response.GenerateContentResponse

def checkFinishReason(response: GenerateContentResponse): IO[Unit] = {
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
import gemini4s.model.domain.GenerationConfig

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
