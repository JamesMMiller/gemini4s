## Introduction

The **gemini4s** library provides a type-safe, asynchronous, and streaming interface to interact with Gemini generative language models using the power of ZIO. Designed with modularity and scalability in mind, gemini4s leverages a tagless final approach to separate core abstractions from their implementations. This documentation covers the primary modules, configuration, key APIs, error-handling strategies, and usage examples.

## Module Overview

- **Model Package (`gemini4s.model`):**
  - Contains data models representing API requests and responses.
  - Key types include: `Content`, `Part`, `UsageMetadata`, `SafetyRating`, etc.

- **Error Handling (`gemini4s.error`):**
  - Defines custom error types encapsulated by the `GeminiError` ADT.
  - Standard error types: `InvalidRequest`, `ConnectionError`, `NetworkError`, etc.

- **Configuration (`gemini4s.config`):**
  - Houses the `GeminiConfig` class that holds API keys and other configuration settings necessary for authenticating and using the Gemini API.

- **Core API Layer (`gemini4s/GeminiService.scala`):**
  - Declares the abstract interface for interacting with the Gemini service.
  - Exposes methods for synchronous and streaming content generation as well as token counting.

- **Implementation Layer (`gemini4s/interpreter` and `gemini4s/http`):**
  - Provides ZIO-based interpreters such as the `GeminiServiceLive` and HTTP client implementations.
  - Handles HTTP communication, JSON serialization/deserialization, and error mapping.

## Detailed API Reference

### GeminiService

The `GeminiService` trait defines the contract for content generation, streaming responses, and token counting.

#### Methods

- **`generateContent`**
  
  ```scala
  def generateContent(
    contents: List[Content],
    safetySettings: Option[List[SafetySetting]] = None,
    generationConfig: Option[GenerationConfig] = None
  )(using GeminiConfig): Task[Either[GeminiError, GenerateContentResponse]]
  ```
  
  - **Description**: Generates content based on the provided list of `Content` objects. Optionally accepts safety settings and generation configurations.
  - **Parameters**:
    - `contents`: A list of input content items.
    - `safetySettings`: Optional settings to enforce content safety rules.
    - `generationConfig`: Optional configuration such as temperature, max tokens, etc.
  - **Returns**: A ZIO `Task` that resolves to either a `GeminiError` or a `GenerateContentResponse`.

- **`generateContentStream`**
  
  ```scala
  def generateContentStream(
    contents: List[Content],
    safetySettings: Option[List[SafetySetting]] = None,
    generationConfig: Option[GenerationConfig] = None
  )(using GeminiConfig): Task[ZStream[Any, GeminiError, GenerateContentResponse]]
  ```
  
  - **Description**: Streams the content generation response in real-time.
  - **Returns**: A ZIO `ZStream` that emits `GenerateContentResponse` values and may fail with a `GeminiError`.

- **`countTokens`**
  
  ```scala
  def countTokens(contents: List[Content])(using GeminiConfig): Task[Either[GeminiError, Int]]
  ```
  
  - **Description**: Counts tokens for the provided content.
  - **Returns**: A ZIO `Task` that either yields a token count (as an `Int`) or a `GeminiError`.

### GeminiConfig

`GeminiConfig` encapsulates the configuration for accessing the Gemini API.

- **Key Field**:
  - `apiKey: String` – Your unique Gemini API key.
  
Additional fields may be added to support other configuration parameters such as API endpoints, regions, or proxy settings.

### GeminiHttpClient

The `GeminiHttpClient` is responsible for:

- Sending HTTP requests to the Gemini API endpoints.
- Handling HTTP response statuses and converting the process into ZIO streams.
- Mapping response errors to the appropriate `GeminiError` types.

#### Key Responsibilities

- **Request Handling**: Uses a scoped client to perform HTTP requests.
- **Response Processing**: Processes and decodes responses using JSON pipelines.
- **Error Mapping**: Converts server or client errors into domain-specific errors like `InvalidRequest` or `ConnectionError`.

### Error Handling with GeminiError

All errors in gemini4s are encapsulated by the `GeminiError` type, which simplifies downstream error handling.

- **Primary Error Variants**:
  - `InvalidRequest`: Represents issues related to request validation or a malformed response.
  - `ConnectionError`: Indicates failures related to network connectivity.
  - `NetworkError`: Covers a broader range of network-related issues not specific to connection problems.
- **Helper Functions**:
  - Use `fromThrowable` to convert generic exceptions into `GeminiError` instances, ensuring consistent error representation.

In addition, you can leverage ZIO's powerful error handling combinators to manage errors in your functional effects:

- Use `foldZIO`, `catchAll`, or `mapError` to manage errors in your effects. For instance, when handling a content generation task:

  ```scala
  service.generateContent(inputContent)(using config).foldZIO(
    error => Console.printLine(s"Error encountered: ${error.message}"),
    success => Console.printLine(s"Generated content: ${success.candidates.head.content.parts.head.text}")
  )
  ```

- For streaming operations, combine `.catchAll` with ZIO streams to gracefully handle errors:

  ```scala
  service.generateContentStream(inputContent)(using config).catchAll(err =>
    ZStream.succeed(GenerateContentResponse(List(/* provide fallback content or log error */)))
  )
  ```

These strategies ensure robust and consistent error management across both synchronous and streaming interactions.

## Usage Examples

### Basic Content Generation

```
### Streaming Content Generation

```scala
import gemini4s._
import gemini4s.config.GeminiConfig
import zio.stream._
import zio.Runtime.default

val config = GeminiConfig("YOUR_API_KEY")
val inputContent = List(
  Content(parts = List(Part(text = "Tell me an interesting story")))
)

val program = for {
  stream <- GeminiServiceLive.generateContentStream(inputContent)(using config)
  _ <- stream.foreach(response => zio.Console.printLine(response.toString))
} yield ()

// Run the streaming program
default.unsafeRun(program)
```

## Extending and Customizing

- **Custom Implementations**: Developers can extend `GeminiService` by providing their own interpreters if specialized behavior is required. Follow the tagless final design to swap out implementations.
- **Middleware and Interceptors**: Customize the `GeminiHttpClient` to include additional logging, metrics, or error recovery strategies.
- **Testing**: Utilize the ZIO Test framework to write both unit and integration tests to verify your custom behavior.

## CI/CD and Development Notes

- **Code Quality**: Ensure all contributions meet the repository's code quality standards (e.g., SBT linting, 90% test coverage).
- **Pull Requests**: Follow the PR process as defined in `.cursorrules` and `CONTRIBUTING.md` when integrating new features or fixes.
- **Documentation**: Keep this documentation updated with any API changes to help maintain clarity for users and contributors.

## Conclusion

This documentation provides a comprehensive overview of the gemini4s API, including its core abstractions, configuration, and usage examples. For further details, refer to the source code and additional examples provided in the repository. Users and contributors are encouraged to enhance this documentation as the project evolves. For contribution guidelines and further development standards, please refer to CONTRIBUTING.md.