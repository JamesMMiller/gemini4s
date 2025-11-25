# Examples

Complete working examples for common use cases.

## Simple Chatbot

```scala
import cats.effect.{IO, IOApp}
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.domain.{GeminiConstants, ModelName}

object SimpleChatbot extends IOApp.Simple {
  val run: IO[Unit] = {
    val config = GeminiConfig(sys.env.getOrElse("GEMINI_API_KEY", "your-api-key"))
    
    GeminiService.make[IO](config).use { service =>
      service.generateContent(
        GenerateContentRequest(ModelName.Gemini25Flash, List(GeminiService.text("Hello! How are you?")))
      ).flatMap {
        case Right(response) =>
          IO.println(response.candidates.head.content.parts.head)
        case Left(error) =>
          IO.println(s"Error: ${error.message}")
      }
    }
  }
}
```

## Streaming Chat

```scala
import cats.effect.{IO, IOApp, Ref}
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig
import gemini4s.model.domain.{Content, ContentPart, GeminiConstants, ModelName}
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.response.ResponsePart

object StreamingChat extends IOApp.Simple {
  val run: IO[Unit] = {
    val config = GeminiConfig(sys.env.getOrElse("GEMINI_API_KEY", "your-api-key"))
    
    GeminiService.make[IO](config).use { service =>
      def chat(history: Ref[IO, List[Content]]): IO[Unit] = {
        for {
          _ <- IO.print("You: ")
          input <- IO.readLine
          _ <- if (input.toLowerCase == "quit") IO.unit else {
            val userMessage = Content(parts = List(ContentPart(input)), role = Some("user"))
            
            for {
              _ <- history.update(_ :+ userMessage)
              currentHistory <- history.get
              
              _ <- IO.print("Assistant: ")
              response <- service.generateContentStream(
                  GenerateContentRequest(ModelName.Gemini25Flash, currentHistory)
                )
                .map(_.candidates.headOption)
                .unNone
                .map(_.content.parts.headOption)
                .unNone
                .collect { case ResponsePart.Text(text) => text }
                .evalTap(chunk => IO.print(chunk))
                .compile
                .foldMonoid
              
              _ <- IO.println("")
              _ <- history.update(_ :+ Content(parts = List(ContentPart(text = response)), role = Some("model")))
              
              _ <- chat(history)
            } yield ()
          }
        } yield ()
      }
      
      Ref.of[IO, List[Content]](List.empty).flatMap(chat)
    }
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
