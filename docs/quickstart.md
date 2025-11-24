# Quick Start

This guide will help you get started with gemini4s in just a few minutes.

## Prerequisites

- Scala 3.6.2 or higher
- JDK 11 or higher
- A Google Gemini API key ([Get one here](https://makersuite.google.com/app/apikey))

## Installation

Add gemini4s to your `build.sbt`:

```scala
libraryDependencies += "io.github.jamesmmiller" %% "gemini4s" % "@VERSION@"
```

## Getting an API Key

1. Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Click "Create API Key"
3. Copy your API key
4. Store it securely (never commit it to version control!)

## Basic Setup

Here's the minimal setup to start using gemini4s:

```scala mdoc:compile-only
import cats.effect.{IO, IOApp}
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import gemini4s.GeminiService
import gemini4s.interpreter.GeminiServiceImpl
import gemini4s.http.GeminiHttpClient
import gemini4s.config.ApiKey
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.domain.ModelName

object BasicExample extends IOApp.Simple {
  val run: IO[Unit] = {
    HttpClientFs2Backend.resource[IO]().use { backend =>
      // 1. Configure
      val apiKey = ApiKey.unsafe("YOUR_API_KEY")
      
      // 2. Create Service
      val httpClient = GeminiHttpClient.make[IO](backend, apiKey)
      val service = GeminiServiceImpl.make[IO](httpClient)
      
      // 3. Use
      val request = GenerateContentRequest(
        model = ModelName.Gemini25Flash,
        contents = List(GeminiService.text("Hello, Gemini!"))
      )

      service.generateContent(request).flatMap {
        case Right(result) => 
          IO.println(result.candidates.head.content.parts.head)
        case Left(error) => 
          IO.println(s"Error: ${error.message}")
      }
    }
  }
}
```

## Configuration Best Practices

### Environment Variables

Don't hardcode your API key! Use environment variables:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.config.ApiKey

val apiKey: IO[ApiKey] = IO {
  val apiKey = sys.env.getOrElse(
    "GEMINI_API_KEY",
    throw new RuntimeException("GEMINI_API_KEY not set")
  )
  ApiKey.unsafe(apiKey)
}
```

### Using Typesafe Config

For production applications, use a configuration library:

```scala
// application.conf
gemini {
  api-key = ${?GEMINI_API_KEY}
  base-url = "https://generativelanguage.googleapis.com/v1beta"
}
```

## Common Patterns

### Reusing the Service

Create the service once and reuse it:

```scala mdoc:compile-only
import cats.effect.{IO, Resource}
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import gemini4s.GeminiService
import gemini4s.interpreter.GeminiServiceImpl
import gemini4s.http.GeminiHttpClient
import gemini4s.config.ApiKey
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.domain.ModelName

def makeService(apiKey: ApiKey): Resource[IO, GeminiService[IO]] = {
  HttpClientFs2Backend.resource[IO]().map { backend =>
    val httpClient = GeminiHttpClient.make[IO](backend, apiKey)
    GeminiServiceImpl.make[IO](httpClient)
  }
}

// Use it
val apiKey = ApiKey.unsafe("YOUR_API_KEY")

makeService(apiKey).use { service =>
  for {
    response1 <- service.generateContent(
      GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("First question")))
    )
    response2 <- service.generateContent(
      GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Second question")))
    )
  } yield ()
}
```

### Error Handling

Always handle errors appropriately:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.error.GeminiError

def handleResponse[A](
  response: Either[GeminiError, A]
): IO[A] = response match {
  case Right(value) => IO.pure(value)
  case Left(error: GeminiError.AuthError) =>
    IO.raiseError(new RuntimeException(s"Authentication failed: ${error.message}"))
  case Left(error: GeminiError.RateLimitExceeded) =>
    IO.println("Rate limited, retrying...") *> 
    IO.sleep(scala.concurrent.duration.Duration(1, "second")) *>
    IO.raiseError(new RuntimeException("Retry needed"))
  case Left(error) =>
    IO.raiseError(new RuntimeException(s"API error: ${error.message}"))
}
```

## Next Steps

Now that you have the basics, explore:

- **[Core Concepts](core-concepts.md)** - Understand the library's design
- **[Content Generation](content-generation.md)** - Learn about generation parameters
- **[Streaming](streaming.md)** - Process responses in real-time
- **[Examples](examples.md)** - See complete working examples

## Troubleshooting

### "Invalid API Key" Error

Make sure your API key is correct and has the necessary permissions. Test it in [Google AI Studio](https://makersuite.google.com/app/apikey) first.

### Connection Timeout

The default timeout is 60 seconds. For longer requests, you may need to adjust the backend configuration.

### Rate Limiting

Free tier has rate limits. See the [Gemini API documentation](https://ai.google.dev/pricing) for details on quotas.
