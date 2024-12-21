# gemini4s

A Tagless Final Scala library for the Google Gemini API built on top of ZIO.

## Features (Planned)

- Tagless Final design for maximum flexibility and composability
- ZIO-based implementation for excellent concurrency and resource management
- Type-safe API interactions
- Streaming support for chat completions
- Comprehensive error handling
- Easy integration with existing ZIO applications

## Getting Started

This project is under development. More information will be added soon.

### Prerequisites

- Scala 3.3.1
- SBT
- JDK 11 or higher
- Google Cloud API key for Gemini

## Development Process

### Git Flow

We follow a modified git flow process:
- `main` branch is protected and requires PR review
- Feature branches: `feature/[issue-number]-short-description`
- Bug fixes: `fix/[issue-number]-short-description`
- Releases: `release/v[version]`

All commits should reference issue numbers: `#123: Add feature X`

### Project Board

Development is tracked using our GitHub project board:
1. New work starts in "To Do"
2. Active development in "In Progress"
3. Code review in "Review"
4. Merged and tested work in "Done"

### Testing

We maintain high code quality standards:
- Minimum 90% test coverage (enforced by scoverage)
- Property-based testing with ZIO Test
- Integration tests for external API interactions

To run tests with coverage:
```bash
sbt clean coverage test coverageReport
```

## Contributing

Contributions are welcome! Please follow these steps:

1. Check the project board for available tasks
2. Create a new branch following our naming convention
3. Implement changes with tests (90% coverage required)
4. Submit a PR and link it to the relevant project card
5. Respond to review feedback
6. Squash commits when ready to merge

See our [.cursorrules](.cursorrules) file for detailed development guidelines.

## License

This project is licensed under the Apache 2.0 License - see the LICENSE file for details. 