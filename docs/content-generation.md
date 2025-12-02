# Content Generation

Learn how to generate content with the Gemini API using gemini4s.

## Basic Generation

The simplest way to generate content:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.domain.{GeminiConstants, ModelName, Temperature, TopK, TopP, MimeType}

// Assuming 'service' is available (see Quick Start)
def basic(service: GeminiService[IO]): IO[Unit] = {
  service.generateContent(
    GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Explain photosynthesis")))
  ).flatMap {
    case Right(response) =>
      val text = response.candidates.head.content.flatMap(_.parts.headOption)
      IO.println(text)
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

## Multimodal Input

Gemini models support multimodal input, allowing you to include images and files alongside text.

### Images

Use the `GeminiService.image` helper to include images (Base64 encoded):

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.domain.{Content, ContentPart, ModelName, MimeType}

def describeImage(service: GeminiService[IO]): IO[Unit] = {
  // Base64 encoded image data
  val base64Image = "..." 
  val imagePart = GeminiService.image(base64Image, "image/jpeg")
  val textPart = GeminiService.text("What is in this image?")
  
  service.generateContent(
    GenerateContentRequest(
      model = ModelName.Gemini25Flash,
      contents = List(Content(parts = imagePart.parts ++ textPart.parts))
    )
  ).flatMap {
    case Right(response) =>
      val text = response.candidates.head.content.flatMap(_.parts.headOption).getOrElse("No content")
      IO.println(text)
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

### Files

Use the `GeminiService.file` helper to include files via URI (e.g., from Google Cloud Storage or File API):

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.domain.{Content, ContentPart, ModelName, MimeType}

def analyzeFile(service: GeminiService[IO]): IO[Unit] = {
  val filePart = GeminiService.file("https://example.com/document.pdf", "application/pdf")
  val textPart = GeminiService.text("Summarize this document")
  
  service.generateContent(
    GenerateContentRequest(
      model = ModelName.Gemini25Flash,
      contents = List(Content(parts = filePart.parts ++ textPart.parts))
    )
  ).void
}
```

## Generation Configuration

Control how content is generated with `GenerationConfig`:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain.{GenerationConfig, GeminiConstants, ModelName, Temperature, TopK, TopP, MimeType}
import gemini4s.model.request.GenerateContentRequest

def withConfig(service: GeminiService[IO]): IO[Unit] = {
  val config = GenerationConfig(
    temperature = Some(Temperature.unsafe(0.7f)),      // Creativity (0.0 - 2.0)
    topK = Some(TopK.unsafe(40)),                // Top-k sampling
    topP = Some(TopP.unsafe(0.95f)),             // Nucleus sampling
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
import gemini4s.model.domain.{Temperature, TopK, TopP}

// For factual, consistent responses
val factual = GenerationConfig(temperature = Some(Temperature.unsafe(0.2f)))

// For creative writing
val creative = GenerationConfig(temperature = Some(Temperature.unsafe(1.5f)))
```

### Top-K and Top-P

Control token selection:

- **topK**: Consider only the K most likely tokens
- **topP**: Consider tokens whose cumulative probability is P

```scala mdoc:compile-only
import gemini4s.model.domain.{GenerationConfig, Temperature, TopK, TopP}

// More focused (fewer options)
val focused = GenerationConfig(topK = Some(TopK.unsafe(10)), topP = Some(TopP.unsafe(0.8f)))

// More diverse (more options)
val diverse = GenerationConfig(topK = Some(TopK.unsafe(100)), topP = Some(TopP.unsafe(0.95f)))
```

## System Instructions

Provide system-level instructions that guide the model's behavior:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain.{Content, ContentPart, GeminiConstants, ModelName}
import gemini4s.model.request.GenerateContentRequest

def withSystemInstruction(service: GeminiService[IO]): IO[Unit] = {
  val systemInstruction = Content(
    parts = List(ContentPart.Text(
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
import gemini4s.model.domain.{Content, ContentPart, GeminiConstants, ModelName}
import gemini4s.model.request.GenerateContentRequest

def conversation(service: GeminiService[IO]): IO[Unit] = {
  val history = List(
    Content(parts = List(ContentPart.Text("What is Scala?")), role = Some("user")),
    Content(
      parts = List(ContentPart.Text("Scala is a programming language...")),
      role = Some("model")
    ),
    Content(parts = List(ContentPart.Text("What are its main features?")), role = Some("user"))
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
import gemini4s.model.domain.{GenerationConfig, GeminiConstants, ModelName, Temperature, TopK, TopP, MimeType}
import gemini4s.model.request.GenerateContentRequest

def jsonMode(service: GeminiService[IO]): IO[Unit] = {
  val jsonConfig = GenerationConfig(
    responseMimeType = Some(MimeType.ApplicationJson)
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
      val json = response.candidates.head.content.flatMap(_.parts.headOption)
      IO.println(s"JSON response: $json")
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

## Structured Outputs

For stricter JSON validation, provide a schema using `responseSchema`:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain._
import gemini4s.model.request.GenerateContentRequest

def structuredOutput(service: GeminiService[IO]): IO[Unit] = {
  val schema = Schema(
    `type` = SchemaType.OBJECT,
    properties = Some(Map(
      "name" -> Schema(SchemaType.STRING),
      "age" -> Schema(SchemaType.INTEGER),
      "skills" -> Schema(
        `type` = SchemaType.ARRAY,
        items = Some(Schema(SchemaType.STRING))
      )
    )),
    required = Some(List("name", "age"))
  )

  val config = GenerationConfig(
    responseMimeType = Some(MimeType.ApplicationJson),
    responseSchema = Some(schema)
  )
  
  service.generateContent(
    GenerateContentRequest(
      model = ModelName.Gemini25Flash,
      contents = List(GeminiService.text("Generate a software engineer profile")),
      generationConfig = Some(config)
    )
  ).flatMap {
    case Right(response) =>
      val json = response.candidates.head.content.flatMap(_.parts.headOption)
      IO.println(s"Structured JSON: $json")
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
import gemini4s.model.domain.{GenerationConfig, GeminiConstants, ModelName, Temperature, TopK, TopP, MimeType}
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
        println(s"Candidate: ${candidate.content.flatMap(_.parts.headOption).getOrElse("No content")}")
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
import gemini4s.model.domain.{GeminiConstants, ModelName, Temperature, TopK, TopP, MimeType}

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
import gemini4s.model.domain.{GeminiConstants, ModelName, Temperature, TopK, TopP, MimeType}

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
import gemini4s.model.domain.{GenerationConfig, Temperature, TopP}

object Configs {
  val factual = GenerationConfig(
    temperature = Some(Temperature.unsafe(0.2f)),
    topP = Some(TopP.unsafe(0.8f)),
    maxOutputTokens = Some(1024)
  )
  
  val creative = GenerationConfig(
    temperature = Some(Temperature.unsafe(1.2f)),
    topP = Some(TopP.unsafe(0.95f)),
    maxOutputTokens = Some(2048)
  )
  
  val concise = GenerationConfig(
    temperature = Some(Temperature.unsafe(0.7f)),
    maxOutputTokens = Some(256)
  )
}
```

## Next Steps

- **[Streaming](streaming.md)** - Stream responses in real-time
- **[Function Calling](function-calling.md)** - Use tools and functions
- **[Safety Settings](safety.md)** - Configure content filtering
- **[Examples](examples.md)** - See complete working examples
