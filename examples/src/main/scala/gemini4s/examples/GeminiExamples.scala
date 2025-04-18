package gemini4s.examples

import zio._
import zio.http.Client
import zio.stream.ZStream

import gemini4s.GeminiService
import gemini4s.config.GeminiConfig
import gemini4s.http.GeminiHttpClient
import gemini4s.interpreter.GeminiServiceLive
import gemini4s.model.GeminiRequest._
import gemini4s.error.GeminiError

/**
 * Examples demonstrating various gemini4s features and error handling.
 */
object GeminiExamples extends ZIOAppDefault {

  /**
   * Example showing error handling and recovery strategies.
   */
  def errorHandlingExample(apiKey: String) = {
    val program = for {
      service <- ZIO.service[GeminiService[Task]]
      result <- service.generateContent(
        contents = List(Content(parts = List(Part(text = "Tell me a joke about programming"))))
      )(using GeminiConfig(apiKey))
        .tapError(error => Console.printLine(s"Error occurred: ${error.getCause().getMessage()}"))
        .retry(Schedule.exponential(1.second) && Schedule.recurs(3))
        .catchAll { error =>
          error match {
            case GeminiError.RateLimitExceeded(message, _) => 
              Console.printLine("Rate limit reached, waiting...") *>
              ZIO.sleep(5.seconds) *>
              service.generateContent(
                contents = List(Content(parts = List(Part(text = "Tell me a joke about programming"))))
              )(using GeminiConfig(apiKey))
            case error: GeminiError.NetworkError =>
              Console.printLine("Network error, using fallback...") *>
              ZIO.succeed(Right(fallbackResponse))
            case _ => 
              Console.printLine("Unrecoverable error, terminating") *>
              ZIO.fail(error)
          }
        }
      _ <- result match {
        case Right(response) => Console.printLine(response.candidates.head.content.parts.head.text)
        case Left(error) => Console.printLine(s"Error: ${error.message}")
      }
    } yield ()

    program.provide(
      Client.default,
      GeminiHttpClient.live,
      GeminiServiceLive.live
    )
  }

  /**
   * Example combining streaming, safety settings, and custom configuration.
   */
  def combinedFeaturesExample(apiKey: String, prompt: String) = {
    val safetySettings = List(
      SafetySetting(
        category = HarmCategory.HARASSMENT,
        threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
      ),
      SafetySetting(
        category = HarmCategory.HATE_SPEECH,
        threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
      )
    )

    val generationConfig = GenerationConfig(
      temperature = Some(0.8f),
      topK = Some(10),
      topP = Some(0.9f),
      candidateCount = Some(1),
      maxOutputTokens = Some(2048)
    )

    val program = for {
      service <- ZIO.service[GeminiService[Task]]
      _ <- Console.printLine("Starting content generation with safety checks...")
      
      // First check token count
      tokenCount <- service.countTokens(
        List(Content(parts = List(Part(text = prompt))))
      )(using GeminiConfig(apiKey))
      _ <- tokenCount match {
        case Right(count) => Console.printLine(s"Token count: $count")
        case Left(error) => Console.printLine(s"Token count error: ${error.message}")
      }
      
      // Then stream content with safety and generation settings
      stream <- service.generateContentStream(
        contents = List(Content(parts = List(Part(text = prompt)))),
        safetySettings = Some(safetySettings),
        generationConfig = Some(generationConfig)
      )(using GeminiConfig(apiKey))
      
      _ <- stream
        .tap { response =>
          // Log safety ratings
          val safetyInfo = response.candidates.head.safetyRatings
            .map(ratings => ratings.map(rating => s"${rating.category}: ${rating.probability}").mkString("\n"))
            .mkString("\n")
          Console.printLine(s"Safety Ratings:\n$safetyInfo")
        }
        .map(_.candidates.head.content.parts.head.text)
        .tap(Console.printLine(_))
        .runDrain
        .catchAll { error =>
          Console.printLine(s"Stream error: ${error.getCause().getMessage()}") *>
          ZIO.unit
        }
    } yield ()

    program.provide(
      Client.default,
      GeminiHttpClient.live,
      GeminiServiceLive.live
    )
  }

  private val fallbackResponse = ???  // Implementation omitted for brevity

  def run = {
    val apiKey = sys.env.getOrElse("GEMINI_API_KEY", "")
    val prompt = "Explain how to handle errors in ZIO, with examples"
    
    Console.printLine("Running error handling example...") *>
    errorHandlingExample(apiKey) *>
    Console.printLine("\nRunning combined features example...") *>
    combinedFeaturesExample(apiKey, prompt)
  }
} 