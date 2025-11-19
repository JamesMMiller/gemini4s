# Best Practices

Production-ready patterns and recommendations for using gemini4s.

## Resource Management

### Always Use Resources

```scala mdoc:compile-only
import cats.effect.{IO, Resource}
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import gemini4s.GeminiService
import gemini4s.interpreter.GeminiServiceImpl
import gemini4s.http.GeminiHttpClient

// Good - automatic cleanup
def makeService: Resource[IO, GeminiService[IO]] = {
  HttpClientFs2Backend.resource[IO]().map { backend =>
    val httpClient = GeminiHttpClient.make[IO](backend)
    GeminiServiceImpl.make[IO](httpClient)
  }
}
```

### Reuse Service Instances

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig

// Good - reuse service
def app(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  for {
    _ <- service.generateContent(List(GeminiService.text("First")))
    _ <- service.generateContent(List(GeminiService.text("Second")))
  } yield ()
}

// Avoid - creating service per request
// (Don't create new service instances for each request)
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
import gemini4s.config.GeminiConfig

// Good - from environment
val config: IO[GeminiConfig] = IO {
  GeminiConfig(sys.env("GEMINI_API_KEY"))
}

// Avoid - hardcoded
val bad = GeminiConfig("hardcoded-key")  // Never do this!
```

### Use Sensible Defaults

```scala mdoc:compile-only
import gemini4s.model.GeminiRequest.GenerationConfig

val productionConfig = GenerationConfig(
  temperature = Some(0.7f),
  maxOutputTokens = Some(2048),
  topP = Some(0.95f)
)
```

## Performance

### Batch Requests When Possible

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.GeminiRequest.EmbedContentRequest
import gemini4s.config.GeminiConfig

// Good - batch embeddings
def batchEmbeddings(
  service: GeminiService[IO],
  texts: List[String]
)(using GeminiConfig): IO[Unit] = {
  val requests = texts.map { text =>
    EmbedContentRequest(
      content = GeminiService.text(text),
      model = s"models/${GeminiService.EmbeddingText004}"
    )
  }
  service.batchEmbedContents(requests).void
}
```

### Use Streaming for Long Responses

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig

// Good - stream long responses
def streamLongContent(
  service: GeminiService[IO]
)(using GeminiConfig): IO[Unit] = {
  service.generateContentStream(
    contents = List(GeminiService.text("Write a long article..."))
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
import gemini4s.model.GeminiRequest.{SafetySetting, HarmCategory, HarmBlockThreshold}

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
import gemini4s.config.GeminiConfig
import gemini4s.model.GeminiRequest.Content
import gemini4s.model.GeminiResponse.GenerateContentResponse
import gemini4s.error.GeminiError

class MockGeminiService extends GeminiService[IO] {
  def generateContent(
    contents: List[Content],
    safetySettings: Option[List[gemini4s.model.GeminiRequest.SafetySetting]] = None,
    generationConfig: Option[gemini4s.model.GeminiRequest.GenerationConfig] = None,
    systemInstruction: Option[Content] = None,
    tools: Option[List[gemini4s.model.GeminiRequest.Tool]] = None,
    toolConfig: Option[gemini4s.model.GeminiRequest.ToolConfig] = None
  )(using config: GeminiConfig): IO[Either[GeminiError, GenerateContentResponse]] = {
    // Return mock response
    IO.pure(Right(GenerateContentResponse(
      candidates = List.empty,
      usageMetadata = None,
      modelVersion = Some("mock"),
      promptFeedback = None
    )))
  }
  
  // Implement other methods...
  def generateContentStream(contents: List[Content], safetySettings: Option[List[gemini4s.model.GeminiRequest.SafetySetting]] = None, generationConfig: Option[gemini4s.model.GeminiRequest.GenerationConfig] = None, systemInstruction: Option[Content] = None, tools: Option[List[gemini4s.model.GeminiRequest.Tool]] = None, toolConfig: Option[gemini4s.model.GeminiRequest.ToolConfig] = None)(using config: GeminiConfig) = fs2.Stream.empty
  def countTokens(contents: List[Content])(using config: GeminiConfig) = IO.pure(Right(0))
  def embedContent(content: Content, taskType: Option[gemini4s.model.GeminiRequest.TaskType] = None, title: Option[String] = None, outputDimensionality: Option[Int] = None)(using config: GeminiConfig) = IO.pure(Right(gemini4s.model.GeminiResponse.ContentEmbedding(List.empty)))
  def batchEmbedContents(requests: List[gemini4s.model.GeminiRequest.EmbedContentRequest])(using config: GeminiConfig) = IO.pure(Right(List.empty))
  def createCachedContent(model: String, systemInstruction: Option[Content] = None, contents: Option[List[Content]] = None, tools: Option[List[gemini4s.model.GeminiRequest.Tool]] = None, toolConfig: Option[gemini4s.model.GeminiRequest.ToolConfig] = None, ttl: Option[String] = None, displayName: Option[String] = None)(using config: GeminiConfig) = IO.pure(Right(gemini4s.model.GeminiResponse.CachedContent("", "", "", "", "", None)))
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
  action: IO[Either[gemini4s.error.GeminiError, gemini4s.model.GeminiResponse.GenerateContentResponse]]
): IO[Either[gemini4s.error.GeminiError, gemini4s.model.GeminiResponse.GenerateContentResponse]] = {
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
