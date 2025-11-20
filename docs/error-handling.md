# Error Handling

Comprehensive guide to handling errors in gemini4s.

## Error Hierarchy

gemini4s provides a typed error hierarchy:

```scala
sealed trait GeminiError extends Throwable

// Authentication errors
sealed trait AuthError extends GeminiError
case class InvalidApiKey(message: String, cause: Option[Throwable]) extends AuthError
case class MissingApiKey(message: String, cause: Option[Throwable]) extends AuthError

// Request errors
sealed trait RequestError extends GeminiError
case class RateLimitExceeded(message: String, cause: Option[Throwable]) extends RequestError
case class InvalidRequest(message: String, cause: Option[Throwable]) extends RequestError

// Model errors
sealed trait ModelError extends GeminiError
case class UnsupportedModel(message: String, cause: Option[Throwable]) extends ModelError
case class ModelOverloaded(message: String, cause: Option[Throwable]) extends ModelError

// Content errors
sealed trait ContentError extends GeminiError
case class SafetyThresholdExceeded(message: String, cause: Option[Throwable]) extends ContentError
case class ContentGenerationFailed(message: String, cause: Option[Throwable]) extends ContentError

// Network errors
sealed trait NetworkError extends GeminiError
case class ConnectionError(message: String, cause: Option[Throwable]) extends NetworkError
case class TimeoutError(message: String, cause: Option[Throwable]) extends TimeoutError

// Streaming errors
sealed trait StreamError extends GeminiError
case class StreamInitializationError(message: String, cause: Option[Throwable]) extends StreamError
case class StreamInterrupted(message: String, cause: Option[Throwable]) extends StreamError
```

## Pattern Matching on Errors

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.error.GeminiError

def handleError(error: GeminiError): IO[Unit] = error match {
  case e: GeminiError.InvalidApiKey =>
    IO.println("Invalid API key - check your configuration")
  
  case e: GeminiError.RateLimitExceeded =>
    IO.println("Rate limited - implement backoff and retry")
  
  case e: GeminiError.SafetyThresholdExceeded =>
    IO.println("Content blocked by safety filters")
  
  case e: GeminiError.ModelOverloaded =>
    IO.println("Model overloaded - retry later")
  
  case e: GeminiError.NetworkError =>
    IO.println(s"Network error: ${e.message}")
  
  case e =>
    IO.println(s"Unexpected error: ${e.message}")
}
```

## Retry Strategies

### Exponential Backoff

```scala mdoc:compile-only
import cats.effect.IO
import scala.concurrent.duration._

def retry[A](
  action: IO[Either[gemini4s.error.GeminiError, A]],
  maxRetries: Int = 3,
  initialDelay: FiniteDuration = 1.second
): IO[Either[gemini4s.error.GeminiError, A]] = {
  def attempt(retriesLeft: Int, delay: FiniteDuration): IO[Either[gemini4s.error.GeminiError, A]] = {
    action.flatMap {
      case Right(value) => IO.pure(Right(value))
      case Left(error: gemini4s.error.GeminiError.RateLimitExceeded) if retriesLeft > 0 =>
        IO.println(s"Rate limited, retrying in $delay...") *>
        IO.sleep(delay) *>
        attempt(retriesLeft - 1, delay * 2)
      case Left(error: gemini4s.error.GeminiError.ModelOverloaded) if retriesLeft > 0 =>
        IO.println(s"Model overloaded, retrying in $delay...") *>
        IO.sleep(delay) *>
        attempt(retriesLeft - 1, delay * 2)
      case Left(error) => IO.pure(Left(error))
    }
  }
  
  attempt(maxRetries, initialDelay)
}
```

### Using cats-retry

For production applications, consider using the [cats-retry](https://github.com/cb372/cats-retry) library which provides robust retry policies with exponential backoff, jitter, and more.

```scala
// Add to build.sbt:
libraryDependencies += "com.github.cb372" %% "cats-retry" % "3.1.0"
```

## Circuit Breaker Pattern

```scala mdoc:compile-only
import cats.effect.{IO, Ref}
import scala.concurrent.duration._

case class CircuitBreaker(
  failureThreshold: Int,
  resetTimeout: FiniteDuration
) {
  sealed trait State
  case object Closed extends State
  case class Open(openedAt: Long) extends State
  case object HalfOpen extends State
  
  def protect[A](
    state: Ref[IO, (State, Int)],
    action: IO[Either[gemini4s.error.GeminiError, A]]
  ): IO[Either[gemini4s.error.GeminiError, A]] = {
    for {
      (currentState, failures) <- state.get
      now <- IO.realTime.map(_.toMillis)
      
      result <- currentState match {
        case Open(openedAt) if now - openedAt > resetTimeout.toMillis =>
          // Try again (half-open)
          state.set((HalfOpen, failures)) *>
          attemptAction(state, action)
        
        case Open(_) =>
          IO.pure(Left(gemini4s.error.GeminiError.ModelOverloaded("Circuit breaker is open")))
        
        case _ =>
          attemptAction(state, action)
      }
    } yield result
  }
  
  private def attemptAction[A](
    state: Ref[IO, (State, Int)],
    action: IO[Either[gemini4s.error.GeminiError, A]]
  ): IO[Either[gemini4s.error.GeminiError, A]] = {
    action.flatMap {
      case Right(value) =>
        state.set((Closed, 0)) *> IO.pure(Right(value))
      
      case Left(error) =>
        state.get.flatMap { case (_, failures) =>
          val newFailures = failures + 1
          if (newFailures >= failureThreshold) {
            IO.realTime.flatMap { now =>
              state.set((Open(now.toMillis), newFailures)) *>
              IO.pure(Left(error))
            }
          } else {
            state.set((Closed, newFailures)) *>
            IO.pure(Left(error))
          }
        }
    }
  }
}
```

## Logging Errors

For production applications, use a logging library like [log4cats](https://github.com/typelevel/log4cats) to track errors:

```scala
// Add to build.sbt:
libraryDependencies += "org.typelevel" %% "log4cats-slf4j" % "2.6.0"
```

## Graceful Degradation

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig

def withFallback(
  service: GeminiService[IO],
  prompt: String
)(using GeminiConfig): IO[String] = {
  service.generateContent(
    contents = List(GeminiService.text(prompt))
  ).flatMap {
    case Right(response) =>
      IO.pure(response.candidates.head.content.parts.head.toString)
    
    case Left(_) =>
      // Fallback to a simpler prompt or default value
      IO.pure("I'm sorry, I couldn't generate a response at this time.")
  }
}
```

## Best Practices

### 1. Always Handle Errors

Never ignore `Either[GeminiError, A]`:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig

// Bad - ignores errors
def bad(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  service.generateContent(
    contents = List(GeminiService.text("Hello"))
  ).void  // Loses error information!
}

// Good - handles errors
def good(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  service.generateContent(
    contents = List(GeminiService.text("Hello"))
  ).flatMap {
    case Right(response) => IO.println(response)
    case Left(error) => IO.println(s"Error: ${error.message}")
  }
}
```

### 2. Use EitherT for Composition

```scala mdoc:compile-only
import cats.data.EitherT
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.error.GeminiError
import gemini4s.config.GeminiConfig

def composed(
  service: GeminiService[IO]
)(using GeminiConfig): EitherT[IO, GeminiError, String] = {
  for {
    response1 <- EitherT(service.generateContent(
      contents = List(GeminiService.text("First"))
    ))
    response2 <- EitherT(service.generateContent(
      contents = List(GeminiService.text("Second"))
    ))
  } yield s"${response1.candidates.head} ${response2.candidates.head}"
}
```

### 3. Implement Timeouts

```scala mdoc:compile-only
import cats.effect.IO
import scala.concurrent.duration._

def withTimeout[A](
  action: IO[Either[gemini4s.error.GeminiError, A]],
  timeout: FiniteDuration = 30.seconds
): IO[Either[gemini4s.error.GeminiError, A]] = {
  action.timeout(timeout).attempt.map {
    case Right(result) => result
    case Left(_) => Left(gemini4s.error.GeminiError.TimeoutError())
  }
}
```

### 4. Monitor Error Rates

Track error rates for alerting:

```scala mdoc:compile-only
import cats.effect.{IO, Ref}

case class ErrorMetrics(
  totalRequests: Long,
  errors: Map[String, Long]
)

def trackErrors[A](
  metrics: Ref[IO, ErrorMetrics],
  action: IO[Either[gemini4s.error.GeminiError, A]]
): IO[Either[gemini4s.error.GeminiError, A]] = {
  metrics.update(m => m.copy(totalRequests = m.totalRequests + 1)) *>
  action.flatTap {
    case Left(error) =>
      val errorType = error.getClass.getSimpleName
      metrics.update { m =>
        m.copy(errors = m.errors.updated(errorType, m.errors.getOrElse(errorType, 0L) + 1))
      }
    case Right(_) => IO.unit
  }
}
```

## Next Steps

- **[Best Practices](best-practices.md)** - Production patterns
- **[Examples](examples.md)** - Complete error handling examples
