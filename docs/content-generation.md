# Content Generation

Learn how to generate content with the Gemini API using gemini4s.

## Basic Generation

The simplest way to generate content:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.domain.{GeminiConstants, ModelName, Temperature, TopK, TopP, MimeType}

// Assuming 'service' is available (see Quick Start)
def basic(service: GeminiService[IO]): IO[Unit] = {
  service.generateContent(
    GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Explain photosynthesis")))
  ).flatMap {
    case Right(response) =>
      val text = response.candidates.head.content.flatMap(_.parts.headOption)
      IO.println(text)
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

## Multimodal Input

Gemini models support multimodal input, allowing you to include images and files alongside text.

### Images

Use the `GeminiService.image` helper to include images (Base64 encoded):

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.domain.{Content, ContentPart, ModelName, MimeType}

def describeImage(service: GeminiService[IO]): IO[Unit] = {
  // Base64 encoded image data
  val base64Image = "..." 
  val imagePart = GeminiService.image(base64Image, "image/jpeg")
  val textPart = GeminiService.text("What is in this image?")
  
  service.generateContent(
    GenerateContentRequest(
      model = ModelName.Gemini25Flash,
      contents = List(Content(parts = imagePart.parts ++ textPart.parts))
    )
  ).flatMap {
    case Right(response) =>
      val text = response.candidates.head.content.flatMap(_.parts.headOption).getOrElse("No content")
      IO.println(text)
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

### Files

Use the `GeminiService.file` helper to include files via URI (e.g., from Google Cloud Storage or [File API](files.md)):

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.domain.{Content, ContentPart, ModelName, MimeType}

def analyzeFile(service: GeminiService[IO]): IO[Unit] = {
  val filePart = GeminiService.file("https://example.com/document.pdf", "application/pdf")
  val textPart = GeminiService.text("Summarize this document")
  
  service.generateContent(
    GenerateContentRequest(
      model = ModelName.Gemini25Flash,
      contents = List(Content(parts = filePart.parts ++ textPart.parts))
    )
  ).void
}
```

## Generation Configuration

Control how content is generated with `GenerationConfig`:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain.{GenerationConfig, GeminiConstants, ModelName, Temperature, TopK, TopP, MimeType}
import gemini4s.model.request.GenerateContentRequest

def withConfig(service: GeminiService[IO]): IO[Unit] = {
  val config = GenerationConfig(
    temperature = Some(Temperature.unsafe(0.7f)),      // Creativity (0.0 - 2.0)
    topK = Some(TopK.unsafe(40)),                // Top-k sampling
    topP = Some(TopP.unsafe(0.95f)),             // Nucleus sampling
    maxOutputTokens = Some(1024),   // Max response length
    stopSequences = Some(List("\n\n")) // Stop generation at these sequences
  )
  
  service.generateContent(
    GenerateContentRequest(
      model = ModelName.Gemini25Flash,
      contents = List(GeminiService.text("Write a haiku")),
      generationConfig = Some(config)
    )
  ).void
}
```

### Temperature

Controls randomness in the output:
- **0.0**: Deterministic, focused responses
- **1.0**: Balanced creativity and coherence (default)
- **2.0**: Maximum creativity, less predictable

```scala mdoc:compile-only
import gemini4s.model.domain.GenerationConfig
import gemini4s.model.domain.{Temperature, TopK, TopP}

// For factual, consistent responses
val factual = GenerationConfig(temperature = Some(Temperature.unsafe(0.2f)))

// For creative writing
val creative = GenerationConfig(temperature = Some(Temperature.unsafe(1.5f)))
```

### Top-K and Top-P

Control token selection:

- **topK**: Consider only the K most likely tokens
- **topP**: Consider tokens whose cumulative probability is P

```scala mdoc:compile-only
import gemini4s.model.domain.{GenerationConfig, Temperature, TopK, TopP}

// More focused (fewer options)
val focused = GenerationConfig(topK = Some(TopK.unsafe(10)), topP = Some(TopP.unsafe(0.8f)))

// More diverse (more options)
val diverse = GenerationConfig(topK = Some(TopK.unsafe(100)), topP = Some(TopP.unsafe(0.95f)))
```

## System Instructions

Provide system-level instructions that guide the model's behavior:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain.{Content, ContentPart, GeminiConstants, ModelName}
import gemini4s.model.request.GenerateContentRequest

def withSystemInstruction(service: GeminiService[IO]): IO[Unit] = {
  val systemInstruction = Content(
    parts = List(ContentPart.Text(
      "You are a helpful assistant that always responds in a friendly, " +
      "encouraging tone. Keep responses concise."
    ))
  )
  
  service.generateContent(
    GenerateContentRequest(
      model = ModelName.Gemini25Flash,
      contents = List(GeminiService.text("How do I learn Scala?")),
      systemInstruction = Some(systemInstruction)
    )
  ).void
}
```

## Multi-Turn Conversations

Build conversations by providing message history:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain.{Content, ContentPart, GeminiConstants, ModelName}
import gemini4s.model.request.GenerateContentRequest

def conversation(service: GeminiService[IO]): IO[Unit] = {
  val history = List(
    Content(parts = List(ContentPart.Text("What is Scala?")), role = Some("user")),
    Content(
      parts = List(ContentPart.Text("Scala is a programming language...")),
      role = Some("model")
    ),
    Content(parts = List(ContentPart.Text("What are its main features?")), role = Some("user"))
  )
  
  service.generateContent(
    GenerateContentRequest(ModelName.Gemini25Flash, history)
  ).void
}
```

## JSON Mode

Force the model to output valid JSON:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain.{GenerationConfig, GeminiConstants, ModelName, Temperature, TopK, TopP, MimeType}
import gemini4s.model.request.GenerateContentRequest

def jsonMode(service: GeminiService[IO]): IO[Unit] = {
  val jsonConfig = GenerationConfig(
    responseMimeType = Some(MimeType.ApplicationJson)
  )
  
  service.generateContent(
    GenerateContentRequest(
      model = ModelName.Gemini25Flash,
      contents = List(GeminiService.text(
        "List 5 programming languages with their year of creation in JSON format"
      )),
      generationConfig = Some(jsonConfig)
    )
  ).flatMap {
    case Right(response) =>
      val json = response.candidates.head.content.flatMap(_.parts.headOption)
      IO.println(s"JSON response: $json")
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

## Structured Outputs

For stricter JSON validation, provide a schema using `responseSchema`:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain._
import gemini4s.model.request.GenerateContentRequest

def structuredOutput(service: GeminiService[IO]): IO[Unit] = {
  val schema = Schema(
    `type` = SchemaType.OBJECT,
    properties = Some(Map(
      "name" -> Schema(SchemaType.STRING),
      "age" -> Schema(SchemaType.INTEGER),
      "skills" -> Schema(
        `type` = SchemaType.ARRAY,
        items = Some(Schema(SchemaType.STRING))
      )
    )),
    required = Some(List("name", "age"))
  )

  val config = GenerationConfig(
    responseMimeType = Some(MimeType.ApplicationJson),
    responseSchema = Some(schema)
  )
  
  service.generateContent(
    GenerateContentRequest(
      model = ModelName.Gemini25Flash,
      contents = List(GeminiService.text("Generate a software engineer profile")),
      generationConfig = Some(config)
    )
  ).flatMap {
    case Right(response) =>
      val json = response.candidates.head.content.flatMap(_.parts.headOption)
      IO.println(s"Structured JSON: $json")
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

## Candidate Count

Request multiple response candidates:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain.{GenerationConfig, GeminiConstants, ModelName, Temperature, TopK, TopP, MimeType}
import gemini4s.model.request.GenerateContentRequest

def multipleCandidates(service: GeminiService[IO]): IO[Unit] = {
  val config = GenerationConfig(
    candidateCount = Some(3)  // Get 3 different responses
  )
  
  service.generateContent(
    GenerateContentRequest(
      model = ModelName.Gemini25Flash,
      contents = List(GeminiService.text("Suggest a name for a cat")),
      generationConfig = Some(config)
    )
  ).flatMap {
    case Right(response) =>
      response.candidates.foreach { candidate =>
        println(s"Candidate: ${candidate.content.flatMap(_.parts.headOption).getOrElse("No content")}")
      }
      IO.unit
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

## Token Counting

Count tokens before making a request:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.request.CountTokensRequest
import gemini4s.model.domain.{GeminiConstants, ModelName, Temperature, TopK, TopP, MimeType}

def countTokens(service: GeminiService[IO]): IO[Unit] = {
  val content = GeminiService.text("This is a long prompt...")
  
  service.countTokens(
    CountTokensRequest(ModelName.Gemini25Flash, List(content))
  ).flatMap {
    case Right(count) =>
      IO.println(s"Token count: $count")
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

## Response Metadata

Access usage metadata from responses:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.domain.{GeminiConstants, ModelName, Temperature, TopK, TopP, MimeType}

def checkMetadata(service: GeminiService[IO]): IO[Unit] = {
  service.generateContent(
    GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Hello")))
  ).flatMap {
    case Right(response) =>
      response.usageMetadata match {
        case Some(metadata) =>
          IO.println(s"Prompt tokens: ${metadata.promptTokenCount}") *>
          IO.println(s"Response tokens: ${metadata.candidatesTokenCount}") *>
          IO.println(s"Total tokens: ${metadata.totalTokenCount}")
        case None => IO.unit
      }
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

## Best Practices

### 1. Use Appropriate Temperature

- **Factual tasks**: Low temperature (0.0 - 0.3)
- **Creative tasks**: Medium to high temperature (0.7 - 1.5)
- **Code generation**: Low to medium temperature (0.2 - 0.7)

### 2. Set Max Tokens

Always set `maxOutputTokens` to avoid unexpectedly long responses:

```scala mdoc:compile-only
import gemini4s.model.domain.GenerationConfig

val config = GenerationConfig(
  maxOutputTokens = Some(512)  // Limit response length
)
```

### 3. Handle Partial Responses

Check the `finishReason` to see if the response was truncated:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.model.response.GenerateContentResponse

def checkFinishReason(response: GenerateContentResponse): IO[Unit] = {
  response.candidates.headOption.flatMap(_.finishReason) match {
    case Some("MAX_TOKENS") =>
      IO.println("Response was truncated due to max tokens")
    case Some("SAFETY") =>
      IO.println("Response was blocked by safety filters")
    case Some("STOP") =>
      IO.println("Response completed normally")
    case other =>
      IO.println(s"Finish reason: $other")
  }
}
```

### 4. Reuse Configuration

Create reusable configurations for common use cases:

```scala mdoc:compile-only
import gemini4s.model.domain.{GenerationConfig, Temperature, TopP}

object Configs {
  val factual = GenerationConfig(
    temperature = Some(Temperature.unsafe(0.2f)),
    topP = Some(TopP.unsafe(0.8f)),
    maxOutputTokens = Some(1024)
  )
  
  val creative = GenerationConfig(
    temperature = Some(Temperature.unsafe(1.2f)),
    topP = Some(TopP.unsafe(0.95f)),
    maxOutputTokens = Some(2048)
  )
  
  val concise = GenerationConfig(
    temperature = Some(Temperature.unsafe(0.7f)),
    maxOutputTokens = Some(256)
  )
}
```

### Batch Generation (Async)

For large volumes of requests, use the asynchronous batch generation API. This is ideal for processing hundreds or thousands of requests efficiently.

#### Creating Batch Jobs

**Method 1: Inline Requests**

Submit a list of requests directly:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain._
import gemini4s.model.request._

def inlineBatchJob(service: GeminiService[IO]): IO[Unit] = {
  val requests = List(
    GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Tell me a joke"))),
    GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Write a haiku"))),
    GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Explain quantum computing")))
  )

  service.batchGenerateContent(ModelName.Gemini25Flash, requests).flatMap {
    case Right(job) =>
      IO.println(s"Batch job created: ${job.name}")
      IO.println(s"Initial state: ${job.state}")
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

**Method 2: File-Based Input (GCS)**

For very large batches, use Cloud Storage:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain._

def gcsBatchJob(service: GeminiService[IO]): IO[Unit] = {
  // File should be in JSONL format with one request per line
  val gcsUri = "gs://my-bucket/batch-requests.jsonl"
  
  service.batchGenerateContent(ModelName.Gemini25Flash, gcsUri).flatMap {
    case Right(job) =>
      IO.println(s"File-based batch job created: ${job.name}")
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

**Method 3: File API**

Use files uploaded via the File API:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain._
import java.nio.file.Paths

def fileApiBatchJob(service: GeminiService[IO]): IO[Unit] = {
  val localFile = Paths.get("batch-requests.jsonl")
  
  for {
    // Upload file first
    uploadResult <- service.uploadFile(localFile, mimeType = Some("application/jsonl"))
    fileUri      <- IO.fromEither(uploadResult.map(_.uri))
    
    // Create batch job from uploaded file
    jobResult    <- service.batchGenerateContent(ModelName.Gemini25Flash, fileUri)
    job          <- IO.fromEither(jobResult)
    
    _            <- IO.println(s"Batch job from File API: ${job.name}")
  } yield ()
}
```

#### Polling for Results

Poll the batch job to check status and retrieve results:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain._
import scala.concurrent.duration._

def pollBatchJob(service: GeminiService[IO], jobName: String): IO[Unit] = {
  def checkStatus: IO[BatchJob] = 
    service.getBatchJob(jobName).flatMap(IO.fromEither)
  
  def pollUntilComplete: IO[BatchJob] = 
    checkStatus.flatMap { job =>
      job.state match {
        case BatchJobState.JOB_STATE_SUCCEEDED =>
          IO.println(s"Job completed successfully!") >> IO.pure(job)
        case BatchJobState.JOB_STATE_FAILED =>
          IO.raiseError(new Exception(s"Job failed: ${job.error.map(_.message)}"))
        case BatchJobState.JOB_STATE_CANCELLED =>
          IO.raiseError(new Exception("Job was cancelled"))
        case _ =>
          IO.println(s"Job ${job.state}, waiting...") >>
          IO.sleep(5.seconds) >>
          pollUntilComplete
      }
    }
  
  pollUntilComplete.void
}
```

#### Retrieving Results

When a batch job completes successfully, the results are available in the `BatchJob` response object.

**For Inline Requests**: Results are in `response.inlinedResponses`  
**For File-Based Requests**: Results file URI is in `response.responsesFile`

**Example: Retrieving Inline Results**

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain._

def retrieveInlineResults(service: GeminiService[IO], jobName: String): IO[Unit] = {
  service.getBatchJob(jobName).flatMap {
    case Right(job) if job.state == BatchJobState.JOB_STATE_SUCCEEDED =>
      job.response match {
        case Some(response) =>
          response.inlinedResponses match {
            case Some(inlinedResults) =>
              IO.println(s"Retrieved ${inlinedResults.size} results:") >>
              inlinedResults.zipWithIndex.traverse_ { case (result, index) =>
                result.response match {
                  case Some(generateResponse) =>
                    generateResponse.candidates.headOption match {
                      case Some(candidate) =>
                        candidate.content.flatMap(_.parts.headOption) match {
                          case Some(part: gemini4s.model.domain.ContentPart.Text) =>
                            IO.println(s"Result $index: ${part.text}")
                          case _ =>
                            IO.println(s"Result $index: No text content")
                        }
                      case None =>
                        IO.println(s"Result $index: No candidates")
                    }
                  case None =>
                    result.error match {
                      case Some(error) =>
                        IO.println(s"Result $index failed: ${error.message}")
                      case None =>
                        IO.println(s"Result $index: No response or error")
                    }
                }
              }
            case None =>
              IO.println("No inline responses found")
          }
        case None =>
          IO.println("No response data available")
      }
    case Right(job) if job.state == BatchJobState.JOB_STATE_FAILED =>
      IO.raiseError(new Exception(s"Job failed: ${job.error.map(_.message).getOrElse("Unknown error")}"))
    case Right(job) =>
      IO.println(s"Job not complete yet: ${job.state}")
    case Left(error) =>
      IO.raiseError(new Exception(s"Error retrieving job: ${error.message}"))
  }
}
```

**Example: Retrieving File-Based Results**

For file-based batch jobs, download the results file:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain._

def retrieveFileResults(service: GeminiService[IO], jobName: String): IO[Unit] = {
  service.getBatchJob(jobName).flatMap {
    case Right(job) if job.state == BatchJobState.JOB_STATE_SUCCEEDED =>
      job.response match {
        case Some(response) =>
          response.responsesFile match {
            case Some(fileUri) =>
              IO.println(s"Results file: $fileUri") >>
              IO.println("Download this file to access the JSONL results")
              // You can use the File API to download:
              // service.getFile(fileUri) or similar
            case None =>
              IO.println("No results file found")
          }
        case None =>
          IO.println("No response data available")
      }
    case Right(job) =>
      IO.println(s"Job status: ${job.state}")
    case Left(error) =>
      IO.raiseError(new Exception(s"Error: ${error.message}"))
  }
}
```

**Complete Example with Polling and Results**

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain._
import gemini4s.model.request._
import scala.concurrent.duration._

def completeBatchWorkflow(service: GeminiService[IO]): IO[Unit] = {
  val requests = List(
    GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("What is Scala?"))),
    GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("What is functional programming?")))
  )

  for {
    // 1. Create batch job
    jobResult <- service.batchGenerateContent(ModelName.Gemini25Flash, requests)
    job       <- IO.fromEither(jobResult)
    _         <- IO.println(s"Created job: ${job.name}")

    // 2. Poll until complete
    completedJob <- {
      def poll: IO[BatchJob] =
        service.getBatchJob(job.name).flatMap(IO.fromEither).flatMap { current =>
          current.state match {
            case BatchJobState.JOB_STATE_SUCCEEDED => IO.pure(current)
            case BatchJobState.JOB_STATE_FAILED =>
              IO.raiseError(new Exception(s"Job failed: ${current.error}"))
            case _ =>
              IO.println(s"Status: ${current.state}, waiting...") >>
              IO.sleep(5.seconds) >> poll
          }
        }
      poll
    }

    // 3. Extract and display results
    _ <- completedJob.response match {
      case Some(response) => response.inlinedResponses match {
        case Some(results) =>
          results.zipWithIndex.traverse_ { case (result, i) =>
            result.response match {
              case Some(res) => IO.println(s"Response $i: ${res.text}")
              case None => IO.println(s"Response $i had error: ${result.error}")
            }
          }
        case None => IO.println("No inline responses")
      }
      case None => IO.println("No response data")
    }

    // 4. Clean up
    _ <- service.deleteBatchJob(job.name)
  } yield ()
}
```

#### Managing Batch Jobs

**List All Jobs**

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService

def listAllJobs(service: GeminiService[IO]): IO[Unit] = {
  service.listBatchJobs(pageSize = 10).flatMap {
    case Right(response) =>
      response.batchJobs match {
        case Some(jobs) =>
          jobs.foreach { job =>
            IO.println(s"${job.name}: ${job.state}")
          }
        case None =>
          IO.println("No batch jobs found")
      }
    case Left(error) =>
      IO.println(s"Error listing jobs: ${error.message}")
  }
}
```

**Cancel a Running Job**

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService

def cancelJob(service: GeminiService[IO], jobName: String): IO[Unit] = {
  service.cancelBatchJob(jobName).flatMap {
    case Right(_) =>
      IO.println(s"Job $jobName cancelled successfully")
    case Left(error) =>
      IO.println(s"Error cancelling job: ${error.message}")
  }
}
```

**Delete a Completed Job**

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService

def deleteJob(service: GeminiService[IO], jobName: String): IO[Unit] = {
  service.deleteBatchJob(jobName).flatMap {
    case Right(_) =>
      IO.println(s"Job $jobName deleted successfully")
    case Left(error) =>
      IO.println(s"Error deleting job: ${error.message}")
  }
}
```

#### Batch Job States

| State | Description |
|-------|-------------|
| `JOB_STATE_PENDING` | Job created but not yet started |
| `JOB_STATE_RUNNING` | Job is currently processing |
| `JOB_STATE_SUCCEEDED` | Job completed successfully |
| `JOB_STATE_FAILED` | Job failed with an error |
| `JOB_STATE_CANCELLED` | Job was cancelled |

#### Best Practices

1. **File Format**: When using file-based input, ensure your JSONL file has one request per line
2. **Polling Interval**: Poll every 5-30 seconds depending on batch size
3. **Error Handling**: Always check for `error` field in failed jobs
4. **Cleanup**: Delete completed jobs to avoid quota issues
5. **Monitoring**: List jobs periodically to track overall batch processing status

#### Complete Example

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain._
import gemini4s.model.request._
import scala.concurrent.duration._

def completeBatchExample(service: GeminiService[IO]): IO[Unit] = {
  val requests = List(
    GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Summarize: AI is transforming..."))),
    GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Translate to Spanish: Hello world")))
  )

  for {
    // 1. Create batch job
    jobResult <- service.batchGenerateContent(ModelName.Gemini25Flash, requests)
    job       <- IO.fromEither(jobResult)
    _         <- IO.println(s"Created job: ${job.name}")

    // 2. Poll until complete
    _ <- {
      def poll: IO[BatchJob] =
        service.getBatchJob(job.name).flatMap(IO.fromEither).flatMap { current =>
          current.state match {
            case BatchJobState.JOB_STATE_SUCCEEDED => IO.pure(current)
            case BatchJobState.JOB_STATE_FAILED =>
              IO.raiseError(new Exception(s"Job failed: ${current.error}"))
            case _ =>
              IO.sleep(5.seconds) >> poll
          }
        }
      poll
    }

    _         <- IO.println("Job completed!")

    // 3. Clean up
    _         <- service.deleteBatchJob(job.name)
    _         <- IO.println("Job deleted")
  } yield ()
}
```

```
 
## Next Steps

- **[Streaming](streaming.md)** - Stream responses in real-time
- **[File API](files.md)** - Upload and manage files
- **[Function Calling](function-calling.md)** - Use tools and functions
- **[Safety Settings](safety.md)** - Configure content filtering
- **[Examples](examples.md)** - See complete working examples
