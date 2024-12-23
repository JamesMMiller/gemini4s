package gemini4s.examples

import zio._
import zio.http.Client
import zio.stream.ZStream

import gemini4s.GeminiService
import gemini4s.config.GeminiConfig
import gemini4s.http.GeminiHttpClient
import gemini4s.interpreter.GeminiServiceLive
import gemini4s.model.GeminiRequest._

/**
 * Example CLI application demonstrating gemini4s functionality.
 */
object GeminiCli {
  def parseArgs(args: Array[String]): CliConfig = {
    if (args.length < 3) {
      println("Usage: gemini4s-cli <command> <api-key> <text>")
      println("Commands: generate, count")
      CliConfig()
    } else {
      val command = args(0)
      val apiKey = args(1)
      val text = args(2)

      command match {
        case "generate" => CliConfig(apiKey = apiKey, prompt = text)
        case "count" => CliConfig(apiKey = apiKey, prompt = text)
        case _ => CliConfig()
      }
    }
  }

  def run(config: CliConfig): ZIO[Any, Throwable, ExitCode] = {
    val geminiConfig = GeminiConfig(config.apiKey)

    val program = for {
      service <- ZIO.service[GeminiService[Task]]
      result <- config match {
        case c if c.prompt.nonEmpty && !c.stream =>
          // Generate content
          val generationConfig = GenerationConfig(
            temperature = Some(GeminiService.DefaultTemperature),
            maxOutputTokens = Some(GeminiService.MaxTokensPerRequest)
          )
          val safetySettings = if (c.safety) Some(List(
            SafetySetting(
              category = HarmCategory.HARASSMENT,
              threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
            ),
            SafetySetting(
              category = HarmCategory.HATE_SPEECH,
              threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
            )
          )) else None

          service.generateContent(
            contents = List(Content(parts = List(Part(text = c.prompt)))),
            safetySettings = safetySettings,
            generationConfig = Some(generationConfig)
          )(using geminiConfig).map {
            case Right(response) =>
              response.candidates.headOption.map(_.content.parts.head.text) match {
                case Some(text) => text
                case None => "No response generated"
              }
            case Left(error) => s"Error: ${error.message}"
          }

        case c if c.prompt.nonEmpty && c.stream =>
          // Stream content
          service.generateContentStream(
            contents = List(Content(parts = List(Part(text = c.prompt))))
          )(using geminiConfig).flatMap { stream =>
            stream
              .map { response =>
                response.candidates.headOption.map(_.content.parts.head.text) match {
                  case Some(text) => text
                  case None => "No response generated"
                }
              }
              .tap(chunk => Console.printLine(chunk))
              .runDrain
              .as("Streaming complete")
          }

        case c =>
          // Count tokens
          service.countTokens(List(Content(parts = List(Part(text = c.prompt)))))(using geminiConfig).map {
            case Right(count) => s"Token count: $count"
            case Left(error) => s"Error: ${error.message}"
          }
      }
      _ <- Console.printLine(result)
    } yield ExitCode.success

    program.provide(
      Client.default,
      GeminiHttpClient.live,
      GeminiServiceLive.live
    )
  }

  def main(args: Array[String]): Unit = {
    val config = parseArgs(args)
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.run(
        run(config)
      ).getOrThrowFiberFailure()
    }
  }
}

/**
 * Configuration for the CLI application.
 */
case class CliConfig(
  apiKey: String = "",
  model: String = GeminiService.DefaultModel,
  stream: Boolean = false,
  temperature: Float = GeminiService.DefaultTemperature,
  maxTokens: Int = GeminiService.MaxTokensPerRequest,
  safety: Boolean = false,
  prompt: String = ""
) 