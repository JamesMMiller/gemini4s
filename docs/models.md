# Models

Guide to choosing and using different Gemini models.

## Available Models

For the most up-to-date list of available models, capabilities, and pricing, please refer to the official **[Gemini Models Documentation](https://ai.google.dev/gemini-api/docs/models/gemini)**.

gemini4s provides constants for common models, but you can also pass any valid model string to the service.

```scala mdoc:compile-only
import gemini4s.GeminiService

// Constants for convenience
val flash = GeminiService.Gemini25Flash
val pro = GeminiService.Gemini25Pro
val flashLite = GeminiService.Gemini25FlashLite
val embedding = GeminiService.EmbeddingText004

// You can also use string literals for new models not yet in constants
val futureModel = "models/gemini-3.0-pro"
```
## Using Different Models

Specify the model when creating the service:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.interpreter.GeminiServiceImpl
import gemini4s.http.GeminiHttpClient

// Assuming 'httpClient' is available (see Quick Start)
def createProService(httpClient: GeminiHttpClient[IO]): GeminiService[IO] = {
  // Use Gemini 2.5 Pro explicitly
  GeminiServiceImpl.make[IO](httpClient, GeminiService.Gemini25Pro)
}
```


## Next Steps

- **[Content Generation](content-generation.md)** - Generate content with any model
- **[Embeddings](embeddings.md)** - Use the embedding model
