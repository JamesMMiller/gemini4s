# Streaming

Stream responses in real-time using FS2 for efficient, incremental content generation.

## Why Streaming?

Streaming is useful when:
- You want to display content as it's generated (better UX)
- Processing long responses incrementally
- Building interactive applications (chatbots, assistants)
- Reducing perceived latency

## Basic Streaming

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig

def basicStream(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  service.generateContentStream(
    contents = List(GeminiService.text("Count from 1 to 10"))
  )
    .evalMap(response => IO.println(response.candidates.head.content.parts.head))
    .compile
    .drain
}
```

## Extracting Text from Chunks

Process text as it arrives:

```scala mdoc:compile-only
import cats.effect.IO
import fs2.Stream
import gemini4s.GeminiService
import gemini4s.model.GeminiResponse.ResponsePart
import gemini4s.config.GeminiConfig

def streamText(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  service.generateContentStream(
    contents = List(GeminiService.text("Write a short story"))
  )
    .map(_.candidates.headOption)
    .unNone
    .map(_.content.parts.headOption)
    .unNone
    .collect { case ResponsePart.Text(text) => text }
    .evalMap(text => IO.print(text))  // Print without newlines
    .compile
    .drain
}
```

## Accumulating Responses

Collect the full response while streaming:

```scala mdoc:compile-only
import cats.effect.IO
import cats.effect.Ref
import gemini4s.GeminiService
import gemini4s.model.GeminiResponse.ResponsePart
import gemini4s.config.GeminiConfig

def accumulateStream(service: GeminiService[IO])(using GeminiConfig): IO[String] = {
  for {
    accumulated <- Ref.of[IO, String]("")
    _ <- service.generateContentStream(
      contents = List(GeminiService.text("Explain quantum computing"))
    )
      .map(_.candidates.headOption)
      .unNone
      .map(_.content.parts.headOption)
      .unNone
      .collect { case ResponsePart.Text(text) => text }
      .evalTap(chunk => accumulated.update(_ + chunk))
      .evalMap(chunk => IO.print(chunk))
      .compile
      .drain
    result <- accumulated.get
  } yield result
}
```

## Error Handling in Streams

Handle errors gracefully in streams:

```scala mdoc:compile-only
import cats.effect.IO
import fs2.Stream
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig

def streamWithErrorHandling(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  service.generateContentStream(
    contents = List(GeminiService.text("Hello"))
  )
    .handleErrorWith { error =>
      Stream.eval(IO.println(s"Stream error: ${error.getMessage}")) >>
      Stream.empty
    }
    .compile
    .drain
}
```

## Streaming with Backpressure

FS2 handles backpressure automatically, but you can control it:

```scala mdoc:compile-only
import cats.effect.IO
import scala.concurrent.duration._
import gemini4s.GeminiService
import gemini4s.model.GeminiResponse.ResponsePart
import gemini4s.config.GeminiConfig

def streamWithDelay(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  service.generateContentStream(
    contents = List(GeminiService.text("Count to 20"))
  )
    .map(_.candidates.headOption)
    .unNone
    .map(_.content.parts.headOption)
    .unNone
    .collect { case ResponsePart.Text(text) => text }
    .metered(100.millis)  // Slow down processing
    .evalMap(text => IO.println(text))
    .compile
    .drain
}
```

## Chatbot Example

Build an interactive chatbot with streaming:

```scala mdoc:compile-only
import cats.effect.{IO, Ref}
import gemini4s.GeminiService
import gemini4s.model.GeminiRequest.{Content, Part}
import gemini4s.model.GeminiResponse.ResponsePart
import gemini4s.config.GeminiConfig

def chatbot(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  def chat(history: Ref[IO, List[Content]]): IO[Unit] = {
    for {
      _ <- IO.print("You: ")
      input <- IO.readLine
      _ <- if (input.toLowerCase == "quit") IO.unit else {
        val userMessage = Content(parts = List(Part(input)), role = Some("user"))
        
        for {
          _ <- history.update(_ :+ userMessage)
          currentHistory <- history.get
          
          _ <- IO.print("Assistant: ")
          response <- service.generateContentStream(contents = currentHistory)
            .map(_.candidates.headOption)
            .unNone
            .map(_.content.parts.headOption)
            .unNone
            .collect { case ResponsePart.Text(text) => text }
            .evalMap(chunk => IO.print(chunk))
            .compile
            .foldMonoid
          
          _ <- IO.println()
          assistantMessage = Content(parts = List(Part(response)), role = Some("model"))
          _ <- history.update(_ :+ assistantMessage)
          
          _ <- chat(history)
        } yield ()
      }
    } yield ()
  }
  
  Ref.of[IO, List[Content]](List.empty).flatMap(chat)
}
```

## Streaming with Configuration

Apply generation config to streams:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.GeminiRequest.GenerationConfig
import gemini4s.config.GeminiConfig

def streamWithConfig(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  val config = GenerationConfig(
    temperature = Some(0.8f),
    maxOutputTokens = Some(512)
  )
  
  service.generateContentStream(
    contents = List(GeminiService.text("Write a poem")),
    generationConfig = Some(config)
  )
    .evalMap(response => IO.println(response.candidates.head.content.parts.head))
    .compile
    .drain
}
```

## Monitoring Stream Progress

Track progress as the stream processes:

```scala mdoc:compile-only
import cats.effect.{IO, Ref}
import gemini4s.GeminiService
import gemini4s.model.GeminiResponse.ResponsePart
import gemini4s.config.GeminiConfig

def monitorProgress(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  for {
    chunkCount <- Ref.of[IO, Int](0)
    _ <- service.generateContentStream(
      contents = List(GeminiService.text("Explain machine learning"))
    )
      .map(_.candidates.headOption)
      .unNone
      .map(_.content.parts.headOption)
      .unNone
      .collect { case ResponsePart.Text(text) => text }
      .evalTap(_ => chunkCount.update(_ + 1))
      .evalMap(text => IO.print(text))
      .compile
      .drain
    total <- chunkCount.get
    _ <- IO.println(s"\nReceived $total chunks")
  } yield ()
}
```

## Best Practices

### 1. Always Compile Streams

Streams are lazy - they don't run until compiled:

```scala mdoc:compile-only
import cats.effect.IO
import fs2.Stream

// This does nothing!
val stream: Stream[IO, Int] = Stream(1, 2, 3)

// This runs the stream
val result: IO[Unit] = stream.compile.drain
```

### 2. Handle Errors

Always handle potential errors in streams:

```scala mdoc:compile-only
import cats.effect.IO
import fs2.Stream

def safeStream[A](stream: Stream[IO, A]): Stream[IO, A] = {
  stream.handleErrorWith { error =>
    Stream.eval(IO.println(s"Error: ${error.getMessage}")) >>
    Stream.empty
  }
}
```

### 3. Use Resource Safety

Streams automatically clean up resources:

```scala mdoc:compile-only
import cats.effect.IO
import fs2.Stream

def withResources: Stream[IO, Unit] = {
  Stream.bracket(IO.println("Acquiring"))(
    _ => IO.println("Releasing")
  ).flatMap { _ =>
    Stream.eval(IO.println("Using resource"))
  }
}
```

### 4. Avoid Blocking Operations

Never block in `evalMap`:

```scala mdoc:compile-only
import cats.effect.IO
import fs2.Stream

// Bad - blocks the thread
def bad: Stream[IO, Unit] = {
  Stream(1, 2, 3).evalMap { n =>
    IO(Thread.sleep(1000))  // Don't do this!
  }
}

// Good - uses IO.sleep
def good: Stream[IO, Unit] = {
  Stream(1, 2, 3).evalMap { n =>
    IO.sleep(scala.concurrent.duration.Duration(1, "second"))
  }
}
```

## Next Steps

- **[Function Calling](function-calling.md)** - Use tools with streaming
- **[Error Handling](error-handling.md)** - Handle stream errors
- **[Examples](examples.md)** - Complete streaming examples
- **[Best Practices](best-practices.md)** - Production patterns
