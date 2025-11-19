# Examples

Complete working examples for common use cases.

## Simple Chatbot

```scala
import cats.effect.{IO, IOApp}
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import gemini4s.GeminiService
import gemini4s.interpreter.GeminiServiceImpl
import gemini4s.http.GeminiHttpClient
import gemini4s.config.GeminiConfig

object SimpleChatbot extends IOApp.Simple {
  val run: IO[Unit] = HttpClientFs2Backend.resource[IO]().use { backend =>
    given GeminiConfig = GeminiConfig(sys.env("GEMINI_API_KEY"))
    
    val httpClient = GeminiHttpClient.make[IO](backend)
    val service = GeminiServiceImpl.make[IO](httpClient)
    
    service.generateContent(
      contents = List(GeminiService.text("Hello! How are you?"))
    ).flatMap {
      case Right(response) =>
        IO.println(response.candidates.head.content.parts.head)
      case Left(error) =>
        IO.println(s"Error: ${error.message}")
    }
  }
}
```

## Streaming Chat

```scala
import cats.effect.{IO, IOApp, Ref}
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import gemini4s.GeminiService
import gemini4s.interpreter.GeminiServiceImpl
import gemini4s.http.GeminiHttpClient
import gemini4s.config.GeminiConfig
import gemini4s.model.GeminiRequest.{Content, Part}
import gemini4s.model.GeminiResponse.ResponsePart

object StreamingChat extends IOApp.Simple {
  val run: IO[Unit] = HttpClientFs2Backend.resource[IO]().use { backend =>
    given GeminiConfig = GeminiConfig(sys.env("GEMINI_API_KEY"))
    
    val httpClient = GeminiHttpClient.make[IO](backend)
    val service = GeminiServiceImpl.make[IO](httpClient)
    
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
}
```

## Function Calling Example

See [Function Calling](function-calling.md) for complete examples.

## Semantic Search

See [Embeddings](embeddings.md) for complete examples.

## Next Steps

- **[Quick Start](quickstart.md)** - Get started
- **[Best Practices](best-practices.md)** - Production patterns
