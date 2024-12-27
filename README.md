# gemini4s

A Tagless Final Scala library for the Google Gemini API built on top of ZIO.

[![Scala CI](https://github.com/JamesMMiller/gemini4s/actions/workflows/scala.yml/badge.svg?branch=main)](https://github.com/JamesMMiller/gemini4s/actions/workflows/scala.yml)

## Features (Planned)

- Tagless Final design for maximum flexibility and composability
- ZIO-based implementation for excellent concurrency and resource management
- Type-safe API interactions
- Streaming support for chat completions
- Comprehensive error handling
- Easy integration with existing ZIO applications

## Project Structure

The project is organized into distinct layers:

### Foundation Layer
- Error Types: Comprehensive ADTs for all error cases
- Core Models: Request/response models with JSON codecs

### Core API Layer
- Core Algebra: Tagless final traits defining the API
- HTTP Client: ZIO-based HTTP implementation

### Implementation Layer
- ZIO Interpreter: Complete implementation of the algebra
- Streaming Support: Real-time chat and completions

### Documentation Layer
- Examples: Sample projects and use cases
- API Documentation: Comprehensive guides and references

## Getting Started

This project is under development. More information will be added soon.

### Prerequisites

- Scala 3.3.1
- SBT
- JDK 11 or higher
- Google Cloud API key for Gemini

## Development Process

### Documentation and Workflow Changes

All process and workflow changes must be properly documented:
- Update both `.cursorrules` and `README.md` when changing workflows
- Changes to development rules require PR review
- Keep documentation in sync with project board structure
- Process changes should be reflected in CI/CD pipeline when applicable

### Project Layers and Dependencies

Development follows a structured approach with clear dependencies:
1. Foundation Layer (Error Types, Models)
2. Core API Layer (Algebra, HTTP Client) - depends on Foundation
3. Implementation Layer (ZIO Interpreter) - depends on Core API
4. Documentation Layer - depends on Implementation

### CI/CD Pipeline

All changes must pass through our CI/CD pipeline:
- Automated build and test on every PR
- Scoverage checks (minimum 90% coverage)
- Branch protection rules enforced
- PR reviews required before merge
- No direct commits to main branch allowed

Status checks must pass before merge:
- ✅ Build and Test
- ✅ Coverage Check
- ✅ PR Review

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

Dependencies between tasks are tracked in issue descriptions and must be respected.

### Testing

We maintain high code quality standards:
- Minimum 90% test coverage (enforced by scoverage)
- Property-based testing with ZIO Test
- Integration tests for external API interactions
- Full test suite must pass before PR merge

To run tests with coverage locally:
```bash
sbt clean coverage test coverageReport
```

Coverage reports will be available in `target/scala-3.3.1/scoverage-report/`.

## Contributing

Contributions are welcome! Please follow these steps:

1. Check the project board for available tasks
2. Verify all task dependencies are met
3. Create a new branch following our naming convention
4. Implement changes with tests (90% coverage required)
5. Submit a PR and link it to the relevant project card
6. Ensure all CI checks pass
7. Respond to review feedback
8. Squash commits when ready to merge

See our [.cursorrules](.cursorrules) file for detailed development guidelines.

## License

This project is licensed under the Apache 2.0 License - see the LICENSE file for details. 
