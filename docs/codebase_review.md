# Codebase Review & Improvement Suggestions

This document outlines a review of the `gemini4s` library from the perspective of a professional functional programmer. It covers naming conventions, directory structure, service setup, and implementation details, with specific critiques and recommendations.

## 1. Naming Conventions

### Critique
- **Generic Naming:** The `gemini4s.model` package contains very generic names like `Part`, `Content`, and `Tool`. While these map to the API, they risk collision with other libraries or domain terms in a user's application.
- **Verbs as Types:** The request model `GenerateContent` is named after a verb. In Scala/FP, data types are typically nouns (e.g., `GenerateContentRequest`).
- **Inconsistent Suffixes:** Some requests have the `Request` suffix (`CountTokensRequest`), while others do not (`GenerateContent`).

### Recommendations
- **Rename Request Models:** Standardize on the `Request` suffix.
    - `GenerateContent` -> `GenerateContentRequest`
    - `Part` -> `ContentPart` (to be more specific)
- **Namespace Models:** Consider nesting models under a `domain` object or using more specific package names to avoid pollution.

## 2. Directory Structure

### Critique
- **Cluttered Model Package:** `src/main/scala/gemini4s/model` contains a mix of requests, responses, shared data structures, and configuration objects. This makes it hard to navigate.
- **Interpreter Jargon:** The package `gemini4s.interpreter` uses "Tagless Final" jargon. While accurate, `gemini4s.client` or `gemini4s.impl` is more conventional for the concrete implementation of the service algebra.

### Recommendations
- **Refactor `model` package:**
    - `gemini4s.model.request` - All `*Request` classes.
    - `gemini4s.model.response` - All `*Response` classes.
    - `gemini4s.model.domain` - Shared entities like `Content`, `Part`, `SafetySetting`.
- **Rename `interpreter`:** Rename `gemini4s.interpreter` to `gemini4s.client` to clearly indicate it contains the client implementation.

## 3. Service Setup & Configuration

### Critique
- **Implicit Configuration Injection:** `GeminiService` methods take `(using config: GeminiConfig)`. This implies that configuration (like API keys) might change per-request. In 99% of use cases, the API key is application-scoped. Passing it implicitly to every method adds noise and boilerplate for the user.
- **Default Values in Algebra:** The `GeminiService` trait defines default values (`= None`). While convenient, this can complicate binary compatibility evolution. If you add a parameter, you break the signature.
- **Hardcoded Strings:** `GeminiService.DefaultModel` and endpoint strings are hardcoded.

### Recommendations
- **Constructor Injection:** Move `GeminiConfig` to the `GeminiService.make` (or constructor). The service instance should be fully configured and ready to use.
    ```scala
    // Current
    def generateContent(...)(using config: GeminiConfig): F[...]

    // Recommended
    def generateContent(...): F[...] // Config is captured in the class
    ```
- **Builder Pattern for Requests:** Instead of methods with 7+ `Option` arguments, accept a single `GenerateContentRequest` object. This makes the algebra cleaner and easier to evolve.
    ```scala
    def generateContent(request: GenerateContentRequest): F[...]
    ```

## 4. Implementation Details (Critical)

### Critique
- **"Fake" Streaming:** The `GeminiHttpClient.postStream` implementation buffers the *entire* response into memory before emitting it as a stream.
    ```scala
    // In GeminiHttpClient.scala
    stream.compile.toVector.map(bytes => Stream.emits(bytes).covary[F])
    ```
    This defeats the primary purpose of streaming (low memory footprint, immediate processing). If the response is large, this will cause an `OutOfMemoryError`.
- **Error Handling:** `handleError` catches generic errors and wraps them in `GeminiError.ConnectionError`. This might mask logic errors or OOMs.

### Recommendations
- **Fix Streaming:** Use `fs2` and `sttp` capabilities to stream data as it arrives. Do not compile to vector. Ensure the JSON parsing is also streaming (using `fs2-data-json` or `circe-fs2` properly on the raw byte stream).
- **Type-Safe Model IDs:** Instead of passing `model: String`, define a `ModelId` value class or enum. This prevents typos (e.g., "gemini-pro" vs "gemini-1.5-pro").

## 5. Functional Programming Best Practices

### Critique
- **Tagless Final:** The use of `F[_]` is excellent.
- **Streaming:** The use of `fs2.Stream` in the algebra is excellent.
- **ADTs:** The use of sealed traits for `GeminiRequest` and `ResponsePart` is good practice.

### Summary
The library has a solid foundation with a good FP algebra. The main areas for improvement are:
1.  **Performance/Correctness:** Fix the streaming implementation immediately.
2.  **Usability:** Simplify configuration by moving it to the constructor.
3.  **Consistency:** Standardize naming and organize the directory structure.
