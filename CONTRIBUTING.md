# Contributing to gemini4s

Thank you for your interest in contributing to gemini4s! This document outlines our development process and guidelines.

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

Development is tracked using our [GitHub project board](https://github.com/users/JamesMMiller/projects/3):
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

## Getting Started with Development

1. Check the project board for available tasks
2. Verify all task dependencies are met
3. Create a new branch following our naming convention
4. Implement changes with tests (90% coverage required)
5. Submit a PR and link it to the relevant project card
6. Ensure all CI checks pass
7. Respond to review feedback
8. Squash commits when ready to merge

## Development Guidelines

See our [.cursorrules](.cursorrules) file for detailed development guidelines, including:
- Code style and formatting
- Documentation requirements
- Testing requirements
- PR process
- Review guidelines

## Code Review Process

1. All PRs must be reviewed by at least one maintainer
2. Reviews should focus on:
   - Code correctness
   - Test coverage
   - Documentation quality
   - API design
   - Performance considerations
3. Address all review comments
4. Request re-review after making changes
5. Squash commits before merging

## Documentation Requirements

1. All public APIs must have ScalaDoc comments
2. Update README.md for user-facing changes
3. Include examples for new features
4. Keep CHANGELOG.md up to date
5. Update API reference documentation

## Questions and Support

If you have questions about contributing:
1. Check existing issues and documentation
2. Open a new issue with the "question" label
3. Join our community discussions
4. Tag maintainers for urgent questions

## License

By contributing to gemini4s, you agree that your contributions will be licensed under the Apache 2.0 License. 