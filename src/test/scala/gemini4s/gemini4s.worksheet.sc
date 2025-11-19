import cats.effect.{IO, Ref}
import gemini4s.GeminiService
import gemini4s.model.GeminiRequest.Content
import gemini4s.model.GeminiRequest.Part
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
          _ <- history.update(_ :+ Content(parts = List(Part(text = response)), role = Some("model")))
          
          _ <- chat(history)
        } yield ()
      }
    } yield ()
  }
  
  Ref.of[IO, List[Content]](List.empty).flatMap(chat)
}