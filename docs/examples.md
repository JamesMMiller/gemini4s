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
          IO.println(response.candidates.head.content.flatMap(_.parts.headOption).getOrElse("No content"))
        case Left(error) =>
          IO.println(s"Error: ${error.message}")
      }
    }
  }
}
```
