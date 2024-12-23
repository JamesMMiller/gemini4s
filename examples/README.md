# gemini4s Examples

This module contains example applications demonstrating how to use the gemini4s library.

## CLI Application

A command-line interface for interacting with the Gemini API.

### Building

```bash
sbt "examples/assembly"
```

### Usage

The CLI supports two main commands:

1. Generate Content:
```bash
# Basic generation
./gemini4s-cli generate -k YOUR_API_KEY "Tell me a joke"

# Stream responses
./gemini4s-cli generate -k YOUR_API_KEY -s "Tell me a story"

# With safety settings
./gemini4s-cli generate -k YOUR_API_KEY --safety "Tell me something safe"

# Custom temperature and max tokens
./gemini4s-cli generate -k YOUR_API_KEY -t 0.7 --max-tokens 1000 "Be creative"
```

2. Count Tokens:
```bash
./gemini4s-cli count -k YOUR_API_KEY "Count the tokens in this text"
```

### Options

- `-k, --api-key`: Your Gemini API key (required)
- `-m, --model`: Model to use (default: gemini-pro)
- `-s, --stream`: Stream responses (default: false)
- `-t, --temperature`: Temperature for generation (default: 0.9)
- `-mt, --max-tokens`: Maximum tokens to generate (default: 30720)
- `-sf, --safety`: Enable safety settings (default: false)

### Examples

1. Basic text generation:
```bash
./gemini4s-cli generate -k YOUR_API_KEY "What is the capital of France?"
```

2. Streaming with custom temperature:
```bash
./gemini4s-cli generate -k YOUR_API_KEY -s -t 0.7 "Write a short story about a robot"
```

3. Count tokens in text:
```bash
./gemini4s-cli count -k YOUR_API_KEY "How many tokens are in this sentence?"
```

4. Safe content generation:
```bash
./gemini4s-cli generate -k YOUR_API_KEY --safety "Write a children's story"
```

## Environment Variables

You can set the following environment variables to avoid passing them as command-line arguments:

- `GEMINI_API_KEY`: Your Gemini API key 