# Best Practices

Production-ready patterns and recommendations for using gemini4s.

## Resource Management

### Always Use Resources

```scala mdoc:compile-only
import cats.effect.{IO, Resource}
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig

// Good - automatic cleanup and simplified setup
def makeService(apiKey: String): Resource[IO, GeminiService[IO]] = {
  val config = GeminiConfig(apiKey)
  GeminiService.make[IO](config)
}
```

### Reuse Service Instances

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.ApiKey
import gemini4s.model.domain.ModelName

// Good - reuse service
def app(service: GeminiService[IO])(using apiKey: ApiKey): IO[Unit] = {
  import gemini4s.model.request.GenerateContentRequest
  import gemini4s.model.domain.GeminiConstants
  for {
    _ <- service.generateContent(GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("First"))))
    _ <- service.generateContent(GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Second"))))
  } yield ()
}

// Avoid - creating service per request
// (Don't create new service instances for each request)
```

## Configuration

### Environment Variables

Don't hardcode your API key! Use environment variables:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.config.GeminiConfig

val config: IO[GeminiConfig] = IO {
  val apiKey = sys.env.getOrElse(
    "GEMINI_API_KEY",
    throw new RuntimeException("GEMINI_API_KEY not set")
  )
  GeminiConfig(apiKey)
}
```

### Using Typesafe Config

For production applications, use a configuration library like [PureConfig](https://pureconfig.github.io/) or [Ciris](https://ciris.is/).

```scala
// application.conf
gemini {
  api-key = ${?GEMINI_API_KEY}
  base-url = "https://generativelanguage.googleapis.com/v1beta"
}
```

## Error Handling

### Implement Retry Logic

```scala mdoc:compile-only
import cats.effect.IO
import scala.concurrent.duration._

def withRetry[A](
  action: IO[Either[gemini4s.error.GeminiError, A]],
  maxRetries: Int = 3
): IO[Either[gemini4s.error.GeminiError, A]] = {
  def attempt(retriesLeft: Int): IO[Either[gemini4s.error.GeminiError, A]] = {
    action.flatMap {
      case Right(value) => IO.pure(Right(value))
      case Left(error: gemini4s.error.GeminiError.RateLimitExceeded) if retriesLeft > 0 =>
        IO.sleep(1.second) *> attempt(retriesLeft - 1)
      case Left(error) => IO.pure(Left(error))
    }
  }
  attempt(maxRetries)
}
```

### Use Circuit Breakers

Protect against cascading failures - see [Error Handling](error-handling.md#circuit-breaker-pattern).

## Configuration

### Externalize Configuration

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.config.ApiKey

// Good - from environment
val apiKey: IO[ApiKey] = IO {
  ApiKey.unsafe(sys.env("GEMINI_API_KEY"))
}

// Avoid - hardcoded
val bad = ApiKey.unsafe("hardcoded-key")  // Never do this!
```

### Use Sensible Defaults

```scala mdoc:compile-only
import gemini4s.model.domain.{GenerationConfig, Temperature, TopP}

val productionConfig = GenerationConfig(
  temperature = Some(Temperature.unsafe(0.7f)),
  maxOutputTokens = Some(2048),
  topP = Some(TopP.unsafe(0.95f))
)
```

## Performance

### Batch Requests When Possible

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.request.EmbedContentRequest
import gemini4s.config.ApiKey

// Good - batch embeddings
def batchEmbeddings(
  service: GeminiService[IO],
  texts: List[String]
)(using apiKey: ApiKey): IO[Unit] = {
  import gemini4s.model.domain.GeminiConstants
  import gemini4s.model.request.BatchEmbedContentsRequest
  val requests = texts.map { text =>
    EmbedContentRequest(
      content = GeminiService.text(text),
      model = GeminiConstants.EmbeddingText001
    )
  }
  service.batchEmbedContents(BatchEmbedContentsRequest(GeminiConstants.EmbeddingText001, requests)).void
}
```

### Use Streaming for Long Responses

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.ApiKey

// Good - stream long responses
def streamLongContent(
  service: GeminiService[IO]
)(using apiKey: ApiKey): IO[Unit] = {
  import gemini4s.model.request.GenerateContentRequest
  import gemini4s.model.domain.{GeminiConstants, ModelName}
  service.generateContentStream(
    GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Write a long article...")))
  ).compile.drain
}
```

### Cache Embeddings

Embeddings are expensive - cache them aggressively.

## Security

### Never Log API Keys

Never log or print API keys in your application. Store them securely in environment variables or a secrets management system.

### Validate User Input

```scala mdoc:compile-only
import cats.effect.IO

def validateInput(userInput: String): IO[String] = {
  if (userInput.length > 10000) {
    IO.raiseError(new IllegalArgumentException("Input too long"))
  } else if (userInput.trim.isEmpty) {
    IO.raiseError(new IllegalArgumentException("Input cannot be empty"))
  } else {
    IO.pure(userInput)
  }
}
```

### Use Safety Settings

Always configure safety settings in production:

```scala mdoc:compile-only
import gemini4s.model.domain.{SafetySetting, HarmCategory, HarmBlockThreshold}

val productionSafety = List(
  SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE),
  SafetySetting(HarmCategory.HATE_SPEECH, HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE),
  SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE),
  SafetySetting(HarmCategory.DANGEROUS_CONTENT, HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE)
)
```

## Testing

### Use Test Doubles

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.ApiKey
import gemini4s.model.domain.Content
import gemini4s.model.response.GenerateContentResponse
import gemini4s.error.GeminiError

class MockGemini extends GeminiService[IO] {
  def generateContent(request: gemini4s.model.request.GenerateContentRequest): IO[Either[GeminiError, GenerateContentResponse]] = {
    // Return mock response
    IO.pure(Right(GenerateContentResponse(
      candidates = List.empty,
      usageMetadata = None,
      modelVersion = Some("mock"),
      promptFeedback = None
    )))
  }
  
  // Implement other methods...
  def generateContentStream(request: gemini4s.model.request.GenerateContentRequest) = fs2.Stream.empty
  def countTokens(request: gemini4s.model.request.CountTokensRequest) = IO.pure(Right(0))
  def embedContent(request: gemini4s.model.request.EmbedContentRequest) = IO.pure(Right(gemini4s.model.response.ContentEmbedding(List.empty)))
  def batchEmbedContents(request: gemini4s.model.request.BatchEmbedContentsRequest) = IO.pure(Right(List.empty))
  def createCachedContent(request: gemini4s.model.request.CreateCachedContentRequest) = IO.pure(Right(gemini4s.model.response.CachedContent("", "", "", "", "", None)))
}
```

## Monitoring

### Track Metrics

```scala mdoc:compile-only
import cats.effect.{IO, Ref}

case class Metrics(
  requests: Long,
  errors: Long,
  totalTokens: Long
)

def trackMetrics[A](
  metrics: Ref[IO, Metrics],
  action: IO[Either[gemini4s.error.GeminiError, gemini4s.model.response.GenerateContentResponse]]
): IO[Either[gemini4s.error.GeminiError, gemini4s.model.response.GenerateContentResponse]] = {
  metrics.update(m => m.copy(requests = m.requests + 1)) *>
  action.flatTap {
    case Right(response) =>
      response.usageMetadata match {
        case Some(usage) => metrics.update(m => m.copy(totalTokens = m.totalTokens + usage.totalTokenCount))
        case None => IO.unit
      }
    case Left(_) =>
      metrics.update(m => m.copy(errors = m.errors + 1))
  }
}
```

## Next Steps

- **[Error Handling](error-handling.md)** - Comprehensive error strategies
- **[Examples](examples.md)** - Complete working examples
- **[Core Concepts](core-concepts.md)** - Deep dive into the library's design
