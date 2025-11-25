# Models

Guide to choosing and using different Gemini models.

## Available Models

For the most up-to-date list of available models, capabilities, and pricing, please refer to the official **[Gemini Models Documentation](https://ai.google.dev/gemini-api/docs/models/gemini)**.

gemini4s provides constants for common models, but you can also pass any valid model string to the service.

```scala mdoc:compile-only
import gemini4s.model.domain.GeminiConstants

// Constants for convenience
val flash = GeminiConstants.Gemini25Flash
val pro = GeminiConstants.Gemini25Pro
val flashLite = GeminiConstants.Gemini25FlashLite
val embedding = GeminiConstants.EmbeddingText001

// You can also use string literals for new models not yet in constants
val futureModel = "models/gemini-3.0-pro"
```
## Using Different Models

Specify the model when making a request:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.domain.GeminiConstants
import gemini4s.config.GeminiConfig

def useProModel(apiKey: String): IO[Unit] = {
  val config = GeminiConfig(apiKey)
  
  GeminiService.make[IO](config).use { service =>
    // Use Gemini 2.5 Pro explicitly
    service.generateContent(
      GenerateContentRequest(GeminiConstants.Gemini25Pro, List(GeminiService.text("Complex reasoning task")))
    ).void
  }
}
```


## Next Steps

- **[Content Generation](content-generation.md)** - Generate content with any model
- **[Embeddings](embeddings.md)** - Use the embedding model
