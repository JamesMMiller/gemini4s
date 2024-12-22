package gemini4s.examples

import zio._
import zio.cli._
import zio.cli.HelpDoc.Span.text
import zio.http.Client
import zio.stream.ZStream

import gemini4s.GeminiService
import gemini4s.config.GeminiConfig
import gemini4s.http.GeminiHttpClient
import gemini4s.interpreter.GeminiServiceLive
import gemini4s.model.GeminiRequest._

/**
 * Example CLI application demonstrating gemini4s functionality.
 * Features:
 * - Text generation
 * - Streaming responses
 * - Token counting
 * - Safety settings
 */
object GeminiCli extends ZIOCliDefault {

  // CLI options and flags
  val apiKeyOpt = Options.text("api-key").withDefault("").alias("k")
  val modelOpt = Options.text("model").withDefault(GeminiService.DefaultModel).alias("m")
  val streamFlag = Options.boolean("stream").alias("s")
  val temperatureOpt = Options.float("temperature")
    .withDefault(GeminiService.DefaultTemperature)
    .alias("t")
  val maxTokensOpt = Options.integer("max-tokens")
    .withDefault(GeminiService.MaxTokensPerRequest)
    .alias("mt")
  val safetyFlag = Options.boolean("safety").alias("sf")

  // CLI commands
  val generateCommand = Command("generate", apiKeyOpt ++ modelOpt ++ streamFlag ++ temperatureOpt ++ maxTokensOpt ++ safetyFlag)
    .withArgs(Args.text("prompt"))
    .map {
      case (((((apiKey, model), stream), temperature), maxTokens), safety) -> prompt =>
        CliConfig(
          apiKey = apiKey,
          model = model,
          stream = stream,
          temperature = temperature,
          maxTokens = maxTokens,
          safety = safety,
          prompt = prompt
        )
    }

  val countCommand = Command("count", apiKeyOpt ++ modelOpt)
    .withArgs(Args.text("text"))
    .map { case (apiKey, model) -> text =>
      CliConfig(
        apiKey = apiKey,
        model = model,
        prompt = text
      )
    }

  // CLI app
  override val cliApp = CliApp.make(
    name = "gemini4s-cli",
    version = "0.1.0",
    summary = text("Example CLI for gemini4s"),
    command = generateCommand.orElse(countCommand)
  ) { config =>
    val geminiConfig = GeminiConfig(config.apiKey)

    val program = for {
      service <- ZIO.service[GeminiService[Task]]
      result <- config match {
        case c if c.prompt.nonEmpty && !c.stream =>
          // Generate content
          val generationConfig = GenerationConfig(
            temperature = Some(c.temperature),
            maxOutputTokens = Some(c.maxTokens)
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
            contents = List(Content.Text(c.prompt)),
            safetySettings = safetySettings,
            generationConfig = Some(generationConfig)
          ).map {
            case Right(response) =>
              response.candidates.headOption.map(_.content.parts.head) match {
                case Some(part) => part.toString
                case None => "No response generated"
              }
            case Left(error) => s"Error: ${error.message}"
          }

        case c if c.prompt.nonEmpty && c.stream =>
          // Stream content
          service.generateContentStream(
            contents = List(Content.Text(c.prompt))
          ).flatMap { stream =>
            stream
              .map { response =>
                response.candidates.headOption.map(_.content.parts.head) match {
                  case Some(part) => part.toString
                  case None => "No response generated"
                }
              }
              .tap(chunk => Console.printLine(chunk))
              .runDrain
              .as("Streaming complete")
          }

        case c =>
          // Count tokens
          service.countTokens(List(Content.Text(c.prompt))).map {
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