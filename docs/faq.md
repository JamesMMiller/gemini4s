# FAQ

Frequently asked questions about gemini4s.

## General

### What is gemini4s?

gemini4s is a purely functional Scala 3 library for the Google Gemini API, built on the Typelevel stack (Cats Effect, FS2, Sttp, Circe).

### Why use gemini4s instead of calling the API directly?

- **Type safety**: Strongly typed models catch errors at compile time
- **Functional**: Referentially transparent, composable effects
- **Streaming**: Native FS2 streaming support
- **Error handling**: Comprehensive typed error hierarchy
- **Tested**: Well-tested with high coverage

### What Scala versions are supported?

Scala 3.6.2 and higher.

## Setup

### How do I get an API key?

Visit [Google AI Studio](https://makersuite.google.com/app/apikey) and create an API key.

### Where should I store my API key?

Use environment variables or a secure configuration management system. Never commit API keys to version control.

## Usage

### How do I handle rate limiting?

Implement retry logic with exponential backoff. See [Error Handling](error-handling.md#retry-strategies).

### Can I use gemini4s with ZIO?

Yes! gemini4s is effect-polymorphic. You can use it with any effect type that has an `Async` instance, including ZIO.

### How do I stream responses?

Use `generateContentStream`:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig
import gemini4s.model.domain.ModelName

def stream(service: GeminiService[IO]): IO[Unit] = {
  import gemini4s.model.request.GenerateContentRequest
  service.generateContentStream(
    GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Hello")))
  ).compile.drain
}
```

### How do I count tokens before making a request?

Use `countTokens`:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain.ModelName

def count(service: GeminiService[IO]): IO[Unit] = {
  import gemini4s.model.request.CountTokensRequest
  service.countTokens(
    CountTokensRequest(ModelName.Gemini25Flash, List(GeminiService.text("Your prompt")))
  ).flatMap {
    case Right(count) => IO.println(s"Tokens: $count")
    case Left(error) => IO.println(s"Error: ${error.message}")
  }
}
```

## Errors

### Why am I getting "Invalid API Key"?

- Check that your API key is correct
- Ensure the API key has the necessary permissions
- Verify you're not accidentally logging or exposing the key

### What does "Rate Limit Exceeded" mean?

You've hit the API rate limit. Implement retry logic with backpressure. See [Error Handling](error-handling.md).

### How do I handle safety filter blocks?

Check `promptFeedback.blockReason` in the response. Adjust your safety settings or modify the prompt. See [Safety Settings](safety.md).

## Performance

### How can I improve performance?

- Use streaming for long responses
- Batch embeddings requests
- Cache embeddings
- Reuse service instances
- Use appropriate models (Flash Lite for simple tasks)

### Should I create a new service for each request?

No! Create the service once and reuse it. See [Best Practices](best-practices.md#resource-management).

### How do I reduce costs?

- Use Gemini 2.5 Flash Lite for simple tasks
- Set `maxOutputTokens` to limit response length
- Cache embeddings
- Use context caching for repeated prompts

## Advanced

### Can I use custom HTTP clients?

gemini4s uses Sttp, so you can configure the backend. See the [Sttp documentation](https://sttp.softwaremill.com/).

### How do I implement function calling?

See the [Function Calling](function-calling.md) guide for complete examples.

### Can I use multiple models in the same application?

Yes! You can specify the model for each request using the same service instance:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.domain.{GeminiConstants, ModelName}

def multipleModels(apiKey: String): IO[Unit] = {
  val config = GeminiConfig(apiKey)
  
  GeminiService.make[IO](config).use { service =>
    // Use the same service for different models
    val flashRequest = GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Flash")))
    val proRequest = GenerateContentRequest(ModelName.Gemini25Pro, List(GeminiService.text("Pro")))
    
    for {
      _ <- service.generateContent(flashRequest)
      _ <- service.generateContent(proRequest)
    } yield ()
  }
}
```

## Troubleshooting

### My requests are timing out

- Increase the timeout in your HTTP client configuration
- Use streaming for long-running requests
- Check your network connection

### I'm getting JSON parsing errors

This usually indicates an API change. Please [open an issue](https://github.com/JamesMMiller/gemini4s/issues) with the error details.

### The library isn't compiling

- Ensure you're using Scala 3.6.2 or higher
- Check that all dependencies are compatible
- Try `sbt clean compile`

## Contributing

### How can I contribute?

See [CONTRIBUTING.md](https://github.com/JamesMMiller/gemini4s/blob/main/CONTRIBUTING.md) for guidelines.

### I found a bug, what should I do?

[Open an issue](https://github.com/JamesMMiller/gemini4s/issues) with:
- Steps to reproduce
- Expected vs actual behavior
- Scala version and library version
- Error messages or stack traces

## Next Steps

- **[Quick Start](quickstart.md)** - Get started quickly
- **[Examples](examples.md)** - See complete examples
- **[Best Practices](best-practices.md)** - Production patterns
