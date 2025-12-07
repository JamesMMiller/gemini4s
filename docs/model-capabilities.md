# Type-Safe Model Capabilities

This document describes the type-safe model capability system that ensures you only use models with methods they actually support.

## Overview

The `ModelCapabilities` module provides compile-time verification that a model supports the operation you're trying to perform. For example, you can't accidentally call `embedContent` with a model that doesn't support embeddings.

## Quick Start

```scala
import gemini4s.model.domain.ModelCapabilities._
import gemini4s.GeminiService.ops._  // Import type-safe extensions

// Pre-defined models with their capabilities
val textModel = Model.gemini25Flash  // Has: CanGenerate, CanStream, CanCount, CanCache, CanBatch
val embedModel = Model.embeddingGemini001  // Has: CanEmbed, CanCount

// Type-safe methods - the compiler ensures you use the right model:
service.generateWithModel(textModel, contents)     // ✓ Compiles - textModel has CanGenerate
service.embedWithModel(embedModel, content)        // ✓ Compiles - embedModel has CanEmbed

// These would NOT compile:
// service.embedWithModel(textModel, content)      // ✗ textModel doesn't have CanEmbed
// service.generateWithModel(embedModel, contents) // ✗ embedModel doesn't have CanGenerate
```

## Capability Types

| Capability | Description | Methods |
|------------|-------------|---------|
| `CanGenerate` | Text/content generation | `generateContent` |
| `CanStream` | Streaming generation | `streamGenerateContent` (implicit with CanGenerate) |
| `CanEmbed` | Embedding generation | `embedContent`, `batchEmbedContents` |
| `CanCount` | Token counting | `countTokens` |
| `CanCache` | Context caching | `createCachedContent` |
| `CanBatch` | Batch operations | `batchGenerateContent` |
| `CanPredict` | Image generation (Imagen) | `predict` |
| `CanPredictLong` | Video generation (Veo) | `predictLongRunning` |

## Pre-defined Models

```scala
// Full generation capabilities (text models)
Model.gemini25Flash      // Gemini 2.5 Flash
Model.gemini25Pro        // Gemini 2.5 Pro
Model.gemini25FlashLite  // Gemini 2.5 Flash Lite
Model.gemini3Pro         // Gemini 3 Pro
Model.geminiFlashLatest  // Latest Flash
Model.geminiProLatest    // Latest Pro

// Embedding capabilities
Model.embeddingGemini001 // Gemini Embedding 001

// Image generation
Model.imagen4            // Imagen 4
```

## Creating Custom Models

```scala
// If you know your model supports all generation features:
val myModel = Model.generationModel("my-custom-model")

// If you're using an embedding model:
val myEmbedModel = Model.embeddingModel("my-embedding-model")

// If you only need basic generation:
val basicModel = Model.basicModel("some-model")
```

## Type-Safe Evidence

The module provides type classes for each capability:

```scala
// These type classes provide evidence that a model supports an operation
trait SupportsGeneration[M]
trait SupportsEmbedding[M]  
trait SupportsTokenCount[M]
trait SupportsCaching[M]
trait SupportsBatch[M]
```

These can be used in method signatures to enforce capability requirements:

```scala
def myGenerateMethod[M: SupportsGeneration](model: M): IO[Response] = ???

// This compiles:
myGenerateMethod(Model.gemini25Flash)

// This would fail if embeddingModel doesn't have SupportsGeneration evidence:
// myGenerateMethod(Model.embeddingGemini001)
```

## Backwards Compatibility

For backwards compatibility, raw `ModelName` values also work with all type classes. This allows gradual migration to the type-safe API.

## Important Notes

### Streaming Availability

The API's `supportedGenerationMethods` field does NOT list `streamGenerateContent` separately. However, streaming is **always available** when `generateContent` is supported. The `:streamGenerateContent` endpoint is an alternative invocation method, not a separate capability.

### Verification

The library includes automated tests that verify our capability assertions match the actual Google API. Run:

```bash
sbt "integration / testOnly gemini4s.audit.DiscoveryAuditSpec"
```

This will:
1. Fetch the live model list from the API
2. Check that each model has the methods we claim it has
3. Fail if our assertions don't match reality

## Capability Combinations

Common capability combinations are pre-defined as type aliases:

```scala
// Standard text generation
type GenerationCapabilities = CanGenerate & CanStream & CanCount

// Full generation model (Flash/Pro)
type FullGenerationCapabilities = GenerationCapabilities & CanCache & CanBatch

// Embedding model
type EmbeddingCapabilities = CanEmbed & CanCount
```

## Migration Guide

To migrate from raw `ModelName` to type-safe `Model[C]`:

1. Replace `ModelName.Gemini25Flash` with `Model.gemini25Flash`
2. Use `.toModelName` when calling service methods that need `ModelName`
3. Optionally, add capability constraints to your own methods
