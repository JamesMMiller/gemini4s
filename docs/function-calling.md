# Function Calling

Use function calling (tool use) to extend Gemini's capabilities with custom functions.

## What is Function Calling?

Function calling allows the model to:
- Call external APIs
- Query databases
- Perform calculations
- Access real-time data
- Execute custom logic

The model generates function calls, and you execute them and return results.

## Basic Function Declaration

Define a function the model can call:

```scala mdoc:compile-only
import gemini4s.model.domain._

val weatherFunction = FunctionDeclaration(
  name = "get_weather",
  description = "Get the current weather in a given location",
  parameters = Some(Schema(
    `type` = SchemaType.OBJECT,
    properties = Some(Map(
      "location" -> Schema(
        `type` = SchemaType.STRING,
        description = Some("The city and state, e.g. San Francisco, CA")
      ),
      "unit" -> Schema(
        `type` = SchemaType.STRING,
        `enum` = Some(List("celsius", "fahrenheit")),
        description = Some("Temperature unit")
      )
    )),
    required = Some(List("location"))
  ))
)
```

## Using Tools

Provide tools to the model:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.domain._
import gemini4s.model.request.GenerateContentRequest
import gemini4s.config.GeminiConfig

def useTools(apiKey: String): IO[Unit] = {
  val config = GeminiConfig(apiKey)
  
  GeminiService.make[IO](config).use { service =>
    // 1. Define the function
    val weatherFunction = FunctionDeclaration(
      name = "get_weather",
      description = "Get the current weather in a given location",
      parameters = Some(Schema(
        `type` = SchemaType.OBJECT,
        properties = Some(Map(
          "location" -> Schema(
            `type` = SchemaType.STRING,
            description = Some("The city and state, e.g. San Francisco, CA")
          )
        )),
        required = Some(List("location"))
      ))
    )
    
    // 2. Create the tool
    val weatherTool = Tool(
      functionDeclarations = Some(List(weatherFunction))
    )
    
    // 3. Configure usage
    val toolConfig = ToolConfig(
      functionCallingConfig = Some(FunctionCallingConfig(
        mode = Some(FunctionCallingMode.AUTO)
      ))
    )
    
    // 4. Use in request
    service.generateContent(
      GenerateContentRequest(
        ModelName.Gemini25Flash,
        List(GeminiService.text("What's the weather in Tokyo?")),
        tools = Some(List(weatherTool)),
        toolConfig = Some(toolConfig)
      )
    ).void
  }
}
```

## Function Calling Modes

Control when the model uses functions:

```scala mdoc:compile-only
import gemini4s.model.domain._

// AUTO: Model decides when to call functions
val autoMode = FunctionCallingConfig(
  mode = Some(FunctionCallingMode.AUTO)
)

// ANY: Model must call a function
val anyMode = FunctionCallingConfig(
  mode = Some(FunctionCallingMode.ANY)
)

// NONE: Model cannot call functions
val noneMode = FunctionCallingConfig(
  mode = Some(FunctionCallingMode.NONE)
)
```

## Handling Function Calls

Extract and execute function calls:

```scala mdoc:compile-only
import cats.effect.IO
import io.circe.Json
import gemini4s.model.response.{GenerateContentResponse, ResponsePart}

def handleFunctionCalls(response: GenerateContentResponse): IO[Unit] = {
  val parts = response.candidates.headOption.map(_.content.parts).getOrElse(List.empty)
  parts.foreach {
    case ResponsePart.FunctionCall(data) =>
      println(s"Function: ${data.name}")
      println(s"Arguments: ${data.args}")
    case ResponsePart.Text(text) =>
      println(s"Text: $text")
    case _ =>
      ()
  }
  IO.unit
}
```

## Complete Function Calling Flow

Full example with function execution:

```scala mdoc:compile-only
import cats.effect.IO
import io.circe.Json
import gemini4s.GeminiService
import gemini4s.model.domain._
import gemini4s.model.request.GenerateContentRequest
import gemini4s.model.response.{ResponsePart, GenerateContentResponse}
import gemini4s.config.GeminiConfig

def weatherAgent(apiKey: String): IO[Unit] = {
  val config = GeminiConfig(apiKey)
  
  GeminiService.make[IO](config).use { service =>
    // 1. Define the function
    val weatherFunction = FunctionDeclaration(
      name = "get_weather",
      description = "Get current weather for a location",
      parameters = Some(Schema(
        `type` = SchemaType.OBJECT,
        properties = Some(Map(
          "location" -> Schema(`type` = SchemaType.STRING)
        )),
        required = Some(List("location"))
      ))
    )
    
    val tool = Tool(functionDeclarations = Some(List(weatherFunction)))
    val toolConfig = ToolConfig(
      functionCallingConfig = Some(FunctionCallingConfig(
        mode = Some(FunctionCallingMode.AUTO)
      ))
    )
    
    // 2. Initial request
    service.generateContent(
      GenerateContentRequest(
        ModelName.Gemini25Flash,
        List(GeminiService.text("What's the weather in London?")),
        tools = Some(List(tool)),
        toolConfig = Some(toolConfig)
      )
    ).flatMap {
      case Right(response) =>
        // 3. Check for function calls
        response.candidates.headOption.flatMap(_.content.parts.headOption) match {
          case Some(ResponsePart.FunctionCall(data)) =>
            // 4. Execute the function
            val location = data.args.get("location")
              .flatMap(_.asString)
              .getOrElse("Unknown")
            
            // Simulate API call
            val weatherData = s"Sunny, 22°C in $location"
            
            // 5. Send function result back to model
            val functionResponse = Content(
              parts = List(ContentPart(weatherData)),
              role = Some("function")
            )
            
            service.generateContent(
              GenerateContentRequest(
                ModelName.Gemini25Flash,
                List(
                  GeminiService.text("What's the weather in London?"),
                  functionResponse
                ),
                tools = Some(List(tool)),
                toolConfig = Some(toolConfig)
              )
            ).flatMap {
              case Right(finalResponse) =>
                val text = finalResponse.candidates.headOption
                  .flatMap(_.content.parts.headOption)
                  .collect { case ResponsePart.Text(t) => t }
                  .getOrElse("No response")
                IO.println(text)
              case Left(error) =>
                IO.println(s"Error: ${error.message}")
            }
            
          case Some(ResponsePart.Text(text)) =>
            IO.println(text)
            
          case _ =>
            IO.println("Unexpected response")
        }
        
      case Left(error) =>
        IO.println(s"Error: ${error.message}")
    }
  }
}
```

## Multiple Functions

Provide multiple functions:

```scala mdoc:compile-only
import gemini4s.model.domain._

val weatherFunction = FunctionDeclaration(
  name = "get_weather",
  description = "Get current weather",
  parameters = Some(Schema(
    `type` = SchemaType.OBJECT,
    properties = Some(Map(
      "location" -> Schema(`type` = SchemaType.STRING)
    )),
    required = Some(List("location"))
  ))
)

val timeFunction = FunctionDeclaration(
  name = "get_time",
  description = "Get current time in a timezone",
  parameters = Some(Schema(
    `type` = SchemaType.OBJECT,
    properties = Some(Map(
      "timezone" -> Schema(`type` = SchemaType.STRING)
    )),
    required = Some(List("timezone"))
  ))
)

val tools = Tool(
  functionDeclarations = Some(List(weatherFunction, timeFunction))
)
```

## Restricting Function Calls

Limit which functions can be called:

```scala mdoc:compile-only
import gemini4s.model.domain._

val restrictedConfig = ToolConfig(
  functionCallingConfig = Some(FunctionCallingConfig(
    mode = Some(FunctionCallingMode.ANY),
    allowedFunctionNames = Some(List("get_weather"))  // Only allow this function
  ))
)
```

## Complex Schemas

Define complex parameter schemas:

```scala mdoc:compile-only
import gemini4s.model.domain._

val searchFunction = FunctionDeclaration(
  name = "search_database",
  description = "Search a database with filters",
  parameters = Some(Schema(
    `type` = SchemaType.OBJECT,
    properties = Some(Map(
      "query" -> Schema(
        `type` = SchemaType.STRING,
        description = Some("Search query")
      ),
      "filters" -> Schema(
        `type` = SchemaType.OBJECT,
        properties = Some(Map(
          "category" -> Schema(`type` = SchemaType.STRING),
          "minPrice" -> Schema(`type` = SchemaType.NUMBER),
          "maxPrice" -> Schema(`type` = SchemaType.NUMBER)
        ))
      ),
      "limit" -> Schema(
        `type` = SchemaType.INTEGER,
        description = Some("Maximum results")
      )
    )),
    required = Some(List("query"))
  ))
)
```

## Error Handling

Handle function execution errors:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.model.domain.{Content, ContentPart}

def executeFunction(name: String, args: Map[String, io.circe.Json]): IO[Content] = {
  IO.defer {
    name match {
      case "get_weather" =>
        // Simulate potential failure
        val location = args.get("location").flatMap(_.asString)
        location match {
          case Some(loc) =>
            IO.pure(Content(parts = List(ContentPart(s"Weather data for $loc"))))
          case None =>
            IO.pure(Content(parts = List(ContentPart("Error: location parameter missing"))))
        }
      case unknown =>
        IO.pure(Content(parts = List(ContentPart(s"Error: Unknown function $unknown"))))
    }
  }.handleErrorWith { error =>
    IO.pure(Content(parts = List(ContentPart(s"Error executing function: ${error.getMessage}"))))
  }
}
```

## Best Practices

### 1. Clear Descriptions

Provide clear, detailed descriptions:

```scala mdoc:compile-only
import gemini4s.model.domain._

val goodFunction = FunctionDeclaration(
  name = "calculate_mortgage",
  description = "Calculate monthly mortgage payment given principal, interest rate, and term. " +
                "Returns the monthly payment amount in the same currency as the principal.",
  parameters = Some(Schema(
    `type` = SchemaType.OBJECT,
    properties = Some(Map(
      "principal" -> Schema(
        `type` = SchemaType.NUMBER,
        description = Some("Loan amount in dollars")
      ),
      "annual_rate" -> Schema(
        `type` = SchemaType.NUMBER,
        description = Some("Annual interest rate as a percentage (e.g., 5.5 for 5.5%)")
      ),
      "years" -> Schema(
        `type` = SchemaType.INTEGER,
        description = Some("Loan term in years")
      )
    )),
    required = Some(List("principal", "annual_rate", "years"))
  ))
)
```

### 2. Validate Parameters

Always validate function parameters:

```scala mdoc:compile-only
import cats.effect.IO
import io.circe.Json

def validateAndExecute(name: String, args: Map[String, Json]): IO[String] = {
  name match {
    case "get_weather" =>
      args.get("location").flatMap(_.asString) match {
        case Some(location) if location.nonEmpty =>
          IO.pure(s"Weather for $location")
        case _ =>
          IO.raiseError(new IllegalArgumentException("Invalid location"))
      }
    case _ =>
      IO.raiseError(new IllegalArgumentException(s"Unknown function: $name"))
  }
}
```

### 3. Keep Functions Focused

Each function should do one thing well:

```scala mdoc:compile-only
import gemini4s.model.domain._

// Good - focused functions
val getWeather = FunctionDeclaration(
  name = "get_weather",
  description = "Get current weather",
  parameters = Some(Schema(`type` = SchemaType.OBJECT))
)

val getForecast = FunctionDeclaration(
  name = "get_forecast",
  description = "Get weather forecast",
  parameters = Some(Schema(`type` = SchemaType.OBJECT))
)

// Avoid - function does too much
val weatherEverything = FunctionDeclaration(
  name = "weather_all",
  description = "Get weather, forecast, historical data, and alerts",
  parameters = Some(Schema(`type` = SchemaType.OBJECT))
)
```

### 4. Handle Async Operations

Use IO for async function execution:

```scala mdoc:compile-only
import cats.effect.IO
import sttp.client3._

def callExternalAPI(location: String): IO[String] = {
  IO.defer {
    // Simulate API call
    IO.sleep(scala.concurrent.duration.Duration(100, "milliseconds")) *>
    IO.pure(s"Weather data for $location")
  }
}
```

## Next Steps

- **[Examples](examples.md)** - Complete function calling examples
- **[Streaming](streaming.md)** - Use functions with streaming
- **[Best Practices](best-practices.md)** - Production patterns
