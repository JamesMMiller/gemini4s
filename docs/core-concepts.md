# Core Concepts

Understanding these core concepts will help you use gemini4s effectively.

## Tagless Final Pattern

gemini4s uses the Tagless Final pattern to abstract over effect types. This means the library works with any effect type `F[_]` that has an `Async` instance.

```scala mdoc:compile-only
import cats.effect.Async
import gemini4s.GeminiService

// The service is polymorphic in F[_]
def useService[F[_]: Async](service: GeminiService[F]): F[Unit] = ???
```

**Benefits:**
- **Flexibility**: Use with IO, ZIO, Monix, or custom effect types
- **Testability**: Easy to test with different effect implementations
- **Composability**: Combine with other Tagless Final libraries

## Effect Types

Most operations return `F[Either[GeminiError, A]]`:

```scala
def generateContent(...): F[Either[GeminiError, GenerateContentResponse]]
```

This gives you:
- **Effect tracking**: Side effects are explicit in the type signature
- **Error handling**: Errors are values, not exceptions
- **Composability**: Use `flatMap`, `map`, and other combinators

### Working with Either

```scala mdoc:compile-only
import cats.effect.IO
import cats.syntax.all._
import gemini4s.GeminiService
import gemini4s.error.GeminiError

def example(service: GeminiService[IO])(using config: gemini4s.config.GeminiConfig): IO[String] = {
  service.generateContent(
    contents = List(GeminiService.text("Hello"))
  ).flatMap {
    case Right(response) => 
      IO.pure(response.candidates.head.content.parts.head.toString)
    case Left(error) => 
      IO.raiseError(new RuntimeException(error.message))
  }
}
```

Or use `EitherT` for cleaner composition:

```scala mdoc:compile-only
import cats.data.EitherT
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.error.GeminiError

def exampleWithEitherT(
  service: GeminiService[IO]
)(using config: gemini4s.config.GeminiConfig): EitherT[IO, GeminiError, String] = {
  for {
    response <- EitherT(service.generateContent(
      contents = List(GeminiService.text("Hello"))
    ))
    text = response.candidates.head.content.parts.head.toString
  } yield text
}
```

## Error Hierarchy

gemini4s provides a comprehensive error hierarchy:

```scala
sealed trait GeminiError extends Throwable

// Authentication errors
sealed trait AuthError extends GeminiError
case class InvalidApiKey(...) extends AuthError
case class MissingApiKey(...) extends AuthError

// Request errors
sealed trait RequestError extends GeminiError
case class RateLimitExceeded(...) extends RequestError
case class InvalidRequest(...) extends RequestError

// Model errors
sealed trait ModelError extends GeminiError
case class UnsupportedModel(...) extends ModelError
case class ModelOverloaded(...) extends ModelError

// Content errors
sealed trait ContentError extends GeminiError
case class SafetyThresholdExceeded(...) extends ContentError
case class ContentGenerationFailed(...) extends ContentError

// Network errors
sealed trait NetworkError extends GeminiError
case class ConnectionError(...) extends NetworkError
case class TimeoutError(...) extends NetworkError

// Streaming errors
sealed trait StreamError extends GeminiError
case class StreamInitializationError(...) extends StreamError
case class StreamInterrupted(...) extends StreamError
```

See [Error Handling](error-handling.md) for detailed error handling strategies.

## Configuration

The `GeminiConfig` case class holds API configuration:

```scala mdoc:compile-only
import gemini4s.config.GeminiConfig

val config = GeminiConfig(
  apiKey = "your-api-key",
  baseUrl = "https://generativelanguage.googleapis.com/v1beta" // default
)
```

Configuration is passed implicitly using Scala 3's `given`/`using` syntax:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig

def makeRequest(service: GeminiService[IO]): IO[Unit] = {
  given GeminiConfig = GeminiConfig("your-api-key")
  
  service.generateContent(
    contents = List(GeminiService.text("Hello"))
  ).void
}
```

## Content Model

Content is represented by `Content` and `Part`:

```scala
case class Content(
  parts: List[Part],
  role: Option[String] = None
)

case class Part(text: String)
```

**Helper method** for simple text:

```scala mdoc:compile-only
import gemini4s.GeminiService

val content = GeminiService.text("Hello, world!")
// Equivalent to: Content(parts = List(Part(text = "Hello, world!")))
```

**Multi-turn conversations**:

```scala mdoc:compile-only
import gemini4s.model.GeminiRequest.{Content, Part}

val conversation = List(
  Content(parts = List(Part("What is 2+2?")), role = Some("user")),
  Content(parts = List(Part("4")), role = Some("model")),
  Content(parts = List(Part("What about 3+3?")), role = Some("user"))
)
```

## Resource Management

gemini4s relies on `cats.effect.Resource` for safe resource acquisition and release (like HTTP connection pools).

See the **[Quick Start](quickstart.md#basic-setup)** for the standard resource setup pattern.

## Streaming with FS2

Streaming operations return `Stream[F, A]`:

```scala
def generateContentStream(...): Stream[F, GenerateContentResponse]
```

FS2 streams are:
- **Lazy**: Only evaluated when compiled
- **Resource-safe**: Automatic cleanup
- **Composable**: Rich combinator library

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig

def streamExample(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  service.generateContentStream(
    contents = List(GeminiService.text("Count to 10"))
  )
    .map(_.candidates.head.content.parts.head)
    .evalMap(part => IO.println(part))
    .compile
    .drain
}
```

See [Streaming](streaming.md) for detailed streaming patterns.

## Type Safety

gemini4s uses enums for type-safe constants:

```scala mdoc:compile-only
import gemini4s.model.GeminiRequest._

// Harm categories
val category: HarmCategory = HarmCategory.HARASSMENT

// Block thresholds
val threshold: HarmBlockThreshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE

// Function calling modes
val mode: FunctionCallingMode = FunctionCallingMode.AUTO

// Schema types
val schemaType: SchemaType = SchemaType.STRING
```

This prevents typos and provides IDE autocomplete.

## Next Steps

- **[Content Generation](content-generation.md)** - Learn about generation parameters
- **[Streaming](streaming.md)** - Process responses in real-time
- **[Error Handling](error-handling.md)** - Handle errors gracefully
- **[Best Practices](best-practices.md)** - Production-ready patterns
