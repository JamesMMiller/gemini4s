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
 * 
 * Usage:
 * gemini4s-cli <command> <api-key> <text> [options]
 * 
 * Commands:
 * - generate: Generate content from a prompt
 * - stream: Stream content from a prompt
 * - count: Count tokens in text
 * 
 * Options:
 * --temperature=<value>: Set temperature (0.0-1.0, default 0.8)
 * --max-tokens=<value>: Set max output tokens (default 2048)
 * --safety=<true/false>: Enable/disable safety settings (default false)
 * --model=<model>: Set model name (default gemini-pro)
 */
object GeminiCli {
  def parseArgs(args: Array[String]): CliConfig = {
    if (args.length < 3) {
      println("""
        |Usage: gemini4s-cli <command> <api-key> <text> [options]
        |Commands: 
        |  generate - Generate content from a prompt
        |  stream   - Stream content from a prompt
        |  count    - Count tokens in text
        |
        |Options:
        |  --temperature=<value>  - Set temperature (0.0-1.0, default 0.8)
        |  --max-tokens=<value>   - Set max output tokens (default 2048)
        |  --safety=<true/false>  - Enable/disable safety settings (default false)
        |  --model=<model>        - Set model name (default gemini-pro)
        |""".stripMargin)
      CliConfig()
    } else {
      val command = args(0)
      val apiKey = args(1)
      val text = args(2)
      
      val options = args.drop(3).flatMap { arg =>
        arg.split("=") match {
          case Array("--temperature", value) => Some(("temperature", value))
          case Array("--max-tokens", value) => Some(("maxTokens", value))
          case Array("--safety", value) => Some(("safety", value))
          case Array("--model", value) => Some(("model", value))
          case _ => None
        }
      }.toMap

      val temperature = options.get("temperature").flatMap(_.toFloatOption).getOrElse(GeminiService.DefaultTemperature)
      val maxTokens = options.get("maxTokens").flatMap(_.toIntOption).getOrElse(GeminiService.MaxTokensPerRequest)
      val safety = options.get("safety").flatMap(_.toBooleanOption).getOrElse(false)
      val model = options.getOrElse("model", GeminiService.DefaultModel)

      command match {
        case "generate" => CliConfig(apiKey = apiKey, prompt = text, temperature = temperature, maxTokens = maxTokens, safety = safety, model = model)
        case "stream" => CliConfig(apiKey = apiKey, prompt = text, stream = true, temperature = temperature, maxTokens = maxTokens, safety = safety, model = model)
        case "count" => CliConfig(apiKey = apiKey, prompt = text, model = model)
        case _ => CliConfig()
      }
    }
  }

  def run(config: CliConfig): ZIO[Any, Throwable, ExitCode] = {
    val geminiConfig = GeminiConfig(config.apiKey, config.model)

    val program = for {
      service <- ZIO.service[GeminiService[Task]]
      result <- config match {
        case c if c.prompt.nonEmpty && !c.stream =>
          // Generate content
          val generationConfig = GenerationConfig(
            temperature = Some(c.temperature),
            maxOutputTokens = Some(c.maxTokens),
            topK = Some(10),
            topP = Some(0.9),
            candidateCount = Some(1)
          )
          val safetySettings = if (c.safety) Some(List(
            SafetySetting(
              category = HarmCategory.HARASSMENT,
              threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
            ),
            SafetySetting(
              category = HarmCategory.HATE_SPEECH,
              threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
            ),
            SafetySetting(
              category = HarmCategory.SEXUALLY_EXPLICIT,
              threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
            ),
            SafetySetting(
              category = HarmCategory.DANGEROUS_CONTENT,
              threshold = HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
            )
          )) else None

          service.generateContent(
            contents = List(Content(parts = List(Part(text = c.prompt)))),
            safetySettings = safetySettings,
            generationConfig = Some(generationConfig)
          )(using geminiConfig).map {
            case Right(response) =>
              response.candidates.headOption.map { candidate =>
                val text = candidate.content.parts.head.text
                val safetyInfo = candidate.safetyRatings.map { rating =>
                  s"\nSafety Rating - ${rating.category}: ${rating.probability}"
                }.mkString("\n")
                s"Generated Content:$text\n$safetyInfo"
              }.getOrElse("No response generated")
            case Left(error) => s"Error: ${error.message}"
          }

        case c if c.prompt.nonEmpty && c.stream =>
          // Stream content
          val generationConfig = GenerationConfig(
            temperature = Some(c.temperature),
            maxOutputTokens = Some(c.maxTokens)
          )
          service.generateContentStream(
            contents = List(Content(parts = List(Part(text = c.prompt)))),
            generationConfig = Some(generationConfig)
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
 * 
 * @param apiKey API key for Google Gemini
 * @param model Model name to use (default: gemini-pro)
 * @param stream Whether to stream responses
 * @param temperature Temperature for generation (0.0-1.0)
 * @param maxTokens Maximum tokens to generate
 * @param safety Whether to enable safety settings
 * @param prompt Text prompt to process
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