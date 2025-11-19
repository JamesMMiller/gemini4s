# Context Caching

Context caching allows you to cache frequently used content to reduce costs and latency.

## What is Context Caching?

Context caching stores content (like system instructions, documents, or conversation history) on Google's servers for reuse across multiple requests.

**Benefits:**
- Reduced costs (cached tokens are cheaper)
- Lower latency (cached content doesn't need to be reprocessed)
- Ideal for repeated prompts with large context

## Creating Cached Content

```scala
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig

def createCache(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  val systemInstruction = GeminiService.text(
    "You are a helpful coding assistant specialized in Scala. " +
    "Always provide type-safe, functional solutions."
  )
  
  service.createCachedContent(
    model = GeminiService.Gemini25Flash,
    systemInstruction = Some(systemInstruction),
    ttl = Some("3600s"),  // Cache for 1 hour
    displayName = Some("scala-assistant-context")
  ).flatMap {
    case Right(cached) =>
      IO.println(s"Created cache: ${cached.name}")
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

## When to Use Caching

Use context caching when:
- You have large system instructions (>1000 tokens)
- You're processing the same documents repeatedly
- You have long conversation histories
- Cost optimization is important

## Best Practices

1. **Set appropriate TTL**: Balance between cost and freshness
2. **Cache large, static content**: System instructions, reference documents
3. **Monitor cache usage**: Track cache hits and costs
4. **Update when needed**: Refresh cache when content changes

## Next Steps

- **[Best Practices](best-practices.md)** - Production patterns
