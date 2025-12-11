# API Compliance Audit System

This document describes the automated API compliance audit system used to ensure `gemini4s` stays in sync with the Google Gemini API.

## Overview

The library includes an automated test that fetches the official Google API Discovery Document and compares it against a configuration file that tracks which API features are implemented, ignored, or pending.

This ensures that:
1. **New API features are detected** - When Google adds new methods or fields, CI fails until they are acknowledged
2. **Implementation claims are accurate** - We don't claim to support features we haven't implemented
3. **Missing features are tracked** - Ignored features link to GitHub issues for roadmap visibility

## How It Works

### The Discovery Audit Test

The test is located at:
```
integration/src/test/scala/gemini4s/audit/DiscoveryAuditSpec.scala
```

It performs the following steps:
1. Fetches `https://generativelanguage.googleapis.com/$discovery/rest?version=v1beta`
2. Parses the JSON to extract available resources, methods, and schemas
3. Compares against `compliance_config.json`
4. **Fails** if any untracked features are discovered

### The Compliance Configuration

The configuration file is located at:
```
integration/src/test/resources/compliance_config.json
```

It has the following structure:

```json
{
  "resources": {
    "models": {
      "implemented": ["generateContent", "streamGenerateContent", ...],
      "ignored": [
        { "name": "predict", "reason": "Imagen not implemented", "link": "https://..." }
      ]
    }
  },
  "schemas": {
    "GenerationConfig": {
      "implemented": ["temperature", "topP", ...],
      "ignored": [
        { "name": "thinkingConfig", "reason": "Gemini 2.5+ thinking", "link": "https://..." }
      ]
    }
  }
}
```

### Field Definitions

| Field | Description |
|-------|-------------|
| `implemented` | Array of method/field names that are **fully implemented** in the library |
| `ignored` | Array of objects for features that are **not implemented** |
| `ignored[].name` | The API method or field name |
| `ignored[].reason` | Why this feature is not implemented (e.g., "Legacy API - deprecated") |
| `ignored[].link` | URL to a GitHub issue tracking implementation (empty string if not tracked) |

## API Versions

The library currently uses **v1beta** which provides access to all features:

```json
"apiVersions": {
  "supported": ["v1beta"],
  "ignored": [
    { "name": "v1", "reason": "Stable API - subset of v1beta features", "link": "" },
    { "name": "v1alpha", "reason": "Alpha API - unstable", "link": "" }
  ]
}
```

### Version Differences

| Feature | v1beta | v1 (stable) |
|---------|--------|-------------|
| `cachedContents` (caching) | ✅ | ❌ |
| `files` (file upload) | ✅ | ❌ |
| `media` (media handling) | ✅ | ❌ |
| `operations` (LRO tracking) | ❌ | ✅ |
| `generateAnswer` (AQA) | ✅ | ❌ |
| `predict` (Imagen) | ✅ | ❌ |
| `predictLongRunning` (Veo) | ✅ | ❌ |

### Choosing an API Version

The library provides type-safe version selection through capability modules:

```scala
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig

// Full v1beta service - all capabilities available
GeminiService.make[IO](GeminiConfig.v1beta("key")).use { svc =>
  svc.generateContent(...)     // ✓ GeminiCore
  svc.uploadFile(...)          // ✓ GeminiFiles  
  svc.createCachedContent(...) // ✓ GeminiCaching
  svc.batchGenerateContent(...)// ✓ GeminiBatch
}

// Minimal v1 service - only core capabilities
GeminiService.makeV1[IO]("key").use { svc =>
  svc.generateContent(...)     // ✓ GeminiCore
  // svc.uploadFile(...)       // ✗ Compile error - not available!
}
```

The service type determines what methods are available at compile time:

| Type | Traits Included |
|------|-----------------|
| `GeminiService[F]` (default) | `GeminiCore & GeminiFiles & GeminiCaching & GeminiBatch` |
| `GeminiServiceV1[F]` | `GeminiCore` only |



## Running the Audit Locally

```bash
# Requires GEMINI_API_KEY environment variable
sbt "integration / testOnly gemini4s.audit.DiscoveryAuditSpec"
```

## When the Audit Fails

If the audit fails, you'll see output like:

```
Compliance Audit Failed:
Resource 'models' has unimplemented/untracked methods: newMethod
Schema 'GenerationConfig' has untracked fields: newField

Please update compliance_config.json with 'ignored' entries if these are known missing features.
```

### Available Models

The audit also tracks all available models:

```
Available Models Audit Passed: All 50 models are tracked.
```

If new models are added to the API:

```
Found 2 untracked models:
gemini-3.0-flash
gemini-3.0-pro

Add these to 'availableModels.tracked' or 'availableModels.ignored' in compliance_config.json
```

### How to Fix

1. **If you're implementing the feature**: Add it to the `implemented` array
2. **If it's not being implemented now**: Add it to `ignored` with:
   - A clear `reason`
   - A `link` to a GitHub issue (create one if needed)
3. **If it's a deprecated/legacy feature**: Add to `ignored` with an empty `link`

### Creating a GitHub Issue for Missing Features

```bash
gh issue create \
  --title "Implement [feature name]" \
  --body "## Description\nAdd support for [feature].\n\n## Discovery\nFound via automated API compliance audit." \
  --label enhancement
```

Then add the issue URL to the `link` field in `compliance_config.json`.

## CI Integration

The audit runs on every push and PR via the `api-audit` job in `.github/workflows/ci.yml`.

**⚠️ Important**: The `GEMINI_API_KEY` secret must be configured in the repository settings for CI to work.

## Categories of Ignored Features

### Legacy/Deprecated
Features from the PaLM API that are deprecated:
- `generateText`, `generateMessage`, `embedText`, `batchEmbedText`
- `countTextTokens`, `countMessageTokens`

These have empty `link` fields as they will not be implemented.

### Region-Restricted
Features that are unavailable in certain regions:
- `tunedModels.*` - Model tuning requires special access

### Planned for Future
Features tracked in GitHub issues:
- Image generation (`predict`) - [#76](https://github.com/JamesMMiller/gemini4s/issues/76)
- Video generation (`predictLongRunning`) - [#77](https://github.com/JamesMMiller/gemini4s/issues/77)
- Semantic Retriever - [#79](https://github.com/JamesMMiller/gemini4s/issues/79)
- GenerationConfig enhancements - [#80](https://github.com/JamesMMiller/gemini4s/issues/80)

## Contributing

When adding new features to the library:

1. Check if the feature exists in `compliance_config.json`
2. If it's in `ignored`, move it to `implemented`
3. If it's a completely new feature from the API, add it to `implemented`
4. Run the audit test to verify your changes

When the API changes:

1. Run the audit test to detect new features
2. For each new feature, either:
   - Implement it and add to `implemented`
   - Create a GitHub issue and add to `ignored` with the issue link
3. Update this documentation if new categories of features are added
