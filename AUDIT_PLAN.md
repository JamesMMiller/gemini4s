# Library Audit Plan

Based on the discovery endpoint, the following gaps have been identified and tracked via GitHub issues.

## Resources

### Models Resource
| Method | Status | Issue |
|--------|--------|-------|
| `generateContent` | ✅ Implemented | - |
| `streamGenerateContent` | ✅ Implemented | - |
| `countTokens` | ✅ Implemented | - |
| `embedContent` | ✅ Implemented | - |
| `batchEmbedContents` | ✅ Implemented | - |
| `batchGenerateContent` | ✅ Implemented | - |
| `get` | ❌ Not Implemented | [#81](https://github.com/JamesMMiller/gemini4s/issues/81) |
| `list` | ❌ Not Implemented | [#81](https://github.com/JamesMMiller/gemini4s/issues/81) |
| `generateAnswer` | ❌ Not Implemented | [#75](https://github.com/JamesMMiller/gemini4s/issues/75) |
| `predict` | ❌ Not Implemented | [#76](https://github.com/JamesMMiller/gemini4s/issues/76) |
| `predictLongRunning` | ❌ Not Implemented | [#77](https://github.com/JamesMMiller/gemini4s/issues/77) |
| `asyncBatchEmbedContent` | ❌ Not Implemented | [#78](https://github.com/JamesMMiller/gemini4s/issues/78) |
| `generateMessage` | ⏭️ Deprecated (PaLM) | - |
| `generateText` | ⏭️ Deprecated (PaLM) | - |
| `embedText` | ⏭️ Deprecated (PaLM) | - |
| `batchEmbedText` | ⏭️ Deprecated (PaLM) | - |
| `countTextTokens` | ⏭️ Deprecated (PaLM) | - |
| `countMessageTokens` | ⏭️ Deprecated (PaLM) | - |

### Tuned Models Resource
All tuned model operations are currently disabled as no models support `createTunedModel` in the current API region.

### Corpora Resource (Semantic Retriever)
| Method | Status | Issue |
|--------|--------|-------|
| `create` | ❌ Not Implemented | [#79](https://github.com/JamesMMiller/gemini4s/issues/79) |
| `list` | ❌ Not Implemented | [#79](https://github.com/JamesMMiller/gemini4s/issues/79) |
| `get` | ❌ Not Implemented | [#79](https://github.com/JamesMMiller/gemini4s/issues/79) |
| `delete` | ❌ Not Implemented | [#79](https://github.com/JamesMMiller/gemini4s/issues/79) |
| `patch` | ❌ Not Implemented | [#79](https://github.com/JamesMMiller/gemini4s/issues/79) |
| `query` | ❌ Not Implemented | [#79](https://github.com/JamesMMiller/gemini4s/issues/79) |

## Schemas

### GenerationConfig
| Field | Status | Issue |
|-------|--------|-------|
| `temperature` | ✅ Implemented | - |
| `topP` | ✅ Implemented | - |
| `topK` | ✅ Implemented | - |
| `candidateCount` | ✅ Implemented | - |
| `maxOutputTokens` | ✅ Implemented | - |
| `stopSequences` | ✅ Implemented | - |
| `responseMimeType` | ✅ Implemented | - |
| `responseSchema` | ✅ Implemented | - |
| `presencePenalty` | ❌ Not Implemented | [#80](https://github.com/JamesMMiller/gemini4s/issues/80) |
| `frequencyPenalty` | ❌ Not Implemented | [#80](https://github.com/JamesMMiller/gemini4s/issues/80) |
| `responseLogprobs` | ❌ Not Implemented | [#80](https://github.com/JamesMMiller/gemini4s/issues/80) |
| `logprobs` | ❌ Not Implemented | [#80](https://github.com/JamesMMiller/gemini4s/issues/80) |
| `speechConfig` | ❌ Not Implemented | [#80](https://github.com/JamesMMiller/gemini4s/issues/80) |
| `mediaResolution` | ❌ Not Implemented | [#80](https://github.com/JamesMMiller/gemini4s/issues/80) |
| `thinkingConfig` | ❌ Not Implemented | [#80](https://github.com/JamesMMiller/gemini4s/issues/80) |
| `seed` | ❌ Not Implemented | [#80](https://github.com/JamesMMiller/gemini4s/issues/80) |
| `imageConfig` | ❌ Not Implemented | [#80](https://github.com/JamesMMiller/gemini4s/issues/80) |
| `responseModalities` | ❌ Not Implemented | [#80](https://github.com/JamesMMiller/gemini4s/issues/80) |

## Summary

- **6 methods implemented** for content generation
- **7 new issues created** to track missing features
- **6 legacy methods** deprecated (PaLM API)
- **Automated CI audit** now in place to detect future API changes
