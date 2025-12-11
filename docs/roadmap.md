# Roadmap

This document outlines the future development plans for `gemini4s`.

All items are tracked via GitHub issues and linked to the [API Compliance Audit](api-compliance.md) system.

## High Priority

### Core API Methods
- **List/Get Models** ([#81](https://github.com/JamesMMiller/gemini4s/issues/81)): Programmatically discover available models and their capabilities.
- **GenerationConfig Enhancements** ([#80](https://github.com/JamesMMiller/gemini4s/issues/80)):
  - `presencePenalty` / `frequencyPenalty` - Token repetition penalties
  - `seed` - Deterministic generation
  - `responseLogprobs` / `logprobs` - Log probabilities
  - `thinkingConfig` - Gemini 2.5+ reasoning control
  - `responseModalities` - Multimodal output selection

### Model Constants
- **Comprehensive Model Constants** ([#83](https://github.com/JamesMMiller/gemini4s/issues/83)): Add predefined constants for all 50 available models including Gemini 2.0, Gemma, and specialized variants.

## Medium Priority

### Generation Features
- **Attributed Question Answering (AQA)** ([#75](https://github.com/JamesMMiller/gemini4s/issues/75)): `generateAnswer` API for grounded responses with source attribution.
- **Async Batch Embeddings** ([#78](https://github.com/JamesMMiller/gemini4s/issues/78)): `asyncBatchEmbedContent` for large-scale embedding jobs.

### Media Generation
- **Imagen Image Generation** ([#76](https://github.com/JamesMMiller/gemini4s/issues/76)): 
  - `predict` API for Imagen 4.0 models
  - Support for `imagen-4.0-generate-001`, `imagen-4.0-fast-generate-001`, `imagen-4.0-ultra-generate-001`
- **Veo Video Generation** ([#77](https://github.com/JamesMMiller/gemini4s/issues/77)):
  - `predictLongRunning` API for Veo models
  - Support for `veo-2.0-generate-001`, `veo-3.0-generate-001`
  - Long-running operation (LRO) polling

### Advanced Features
- **Semantic Retrieval (Corpora)** ([#79](https://github.com/JamesMMiller/gemini4s/issues/79)): RAG capabilities with Google's hosted vector store.
- **Grounding**: Integration with Google Search for grounded responses (`googleSearchRetrieval`).
- **Speech/TTS Config**: `speechConfig` for text-to-speech output.

## Future Ideas

- **Live API**: Real-time bidirectional streaming (WebSocket) for conversational AI.
- **Computer Use**: Support for `gemini-2.5-computer-use-preview` model.
- **Robotics**: Support for `gemini-robotics-er-1.5-preview` model.
- **Tuned Models**: Management APIs (currently blocked - no models support `createTunedModel`).

## Not Planned

These features are intentionally not implemented:

- **Legacy PaLM API**: `generateText`, `generateMessage`, `embedText`, `batchEmbedText`, `countTextTokens`, `countMessageTokens` - Deprecated in favor of Gemini API.
- **Model Patching/Deletion**: `patch`, `delete` on models - Not applicable for API consumers.

## Discovery & Tracking

Missing features are automatically detected by the [API Compliance Audit](api-compliance.md) which runs on every CI build. When Google adds new API features, the audit fails until they are acknowledged.

See [compliance_config.json](https://github.com/JamesMMiller/gemini4s/blob/main/integration/src/test/resources/compliance_config.json) for the complete feature tracking configuration.
