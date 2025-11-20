# Models

Guide to choosing and using different Gemini models.

## Available Models

gemini4s provides constants for all Gemini models:

```scala mdoc:compile-only
import gemini4s.GeminiService

// Gemini 2.5 models
val flash = GeminiService.Gemini25Flash        // Default, fast and versatile
val pro = GeminiService.Gemini25Pro            // Complex reasoning
val flashLite = GeminiService.Gemini25FlashLite // Lightweight, high-volume

// Embedding model
val embedding = GeminiService.EmbeddingText004
```

## Model Comparison

| Model | Best For | Speed | Cost | Context Window |
|-------|----------|-------|------|----------------|
| `gemini-2.5-flash` | General purpose, fast responses | Fast | Low | 1M tokens |
| `gemini-2.5-pro` | Complex reasoning, analysis | Medium | Medium | 2M tokens |
| `gemini-2.5-flash-lite` | High-volume, simple tasks | Fastest | Lowest | 1M tokens |
| `text-embedding-004` | Embeddings | Fast | Low | 2048 tokens |

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

## Model Selection Guide

### Use Gemini 2.5 Flash When:
- Building chatbots or assistants
- General Q&A applications
- Content summarization
- Code generation
- Most common use cases

### Use Gemini 2.5 Pro When:
- Complex reasoning tasks
- Deep analysis required
- Multi-step problem solving
- Advanced code understanding
- Research applications

### Use Gemini 2.5 Flash Lite When:
- High-volume, simple requests
- Cost optimization is critical
- Simple classification tasks
- Basic text generation

## Next Steps

- **[Content Generation](content-generation.md)** - Generate content with any model
- **[Embeddings](embeddings.md)** - Use the embedding model
