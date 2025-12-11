# Contributing to gemini4s

Thank you for your interest in contributing to `gemini4s`! We welcome contributions from the community to help make this library better.

## Getting Started

1.  **Fork the repository** on GitHub.
2.  **Clone your fork** locally:
    ```bash
    git clone https://github.com/YOUR_USERNAME/gemini4s.git
    cd gemini4s
    ```
3.  **Create a new branch** for your feature or bug fix:
    ```bash
    git checkout -b feature/my-new-feature
    ```

## Development Workflow

### Prerequisites

-   JDK 11 or higher
-   sbt 1.9.x or higher

### Building and Testing

We use `sbt` for building and testing.

-   **Compile the code**:
    ```bash
    sbt compile
    ```
-   **Run tests**:
    ```bash
    sbt test
    ```
-   **Run integration tests**:
    ```bash
    sbt integration/test
    ```
    > **Note**: Integration tests will run against a mock backend if the `GEMINI_API_KEY` environment variable is not set. To run against the real Gemini API, create a `.env` file with your API key:
    > ```bash
    > GEMINI_API_KEY=your_api_key_here
    > ```
-   **Run linting and coverage checks**:
    ```bash
    sbt lint
    ```

### Documentation

Documentation is built using Typelevel Helium (Laika).

-   **Preview documentation locally**:
    ```bash
    sbt docs/tlSite
    ```
    The generated site will be in `site/target/docs/site`.

## Submitting a Pull Request

1.  Ensure all tests pass and code is formatted correctly (`sbt lint`).
2.  Push your branch to your fork.
3.  Open a Pull Request against the `main` branch of the `gemini4s` repository.
4.  Provide a clear description of your changes and link to any relevant issues.

## API Compliance Audit

This project includes an automated API compliance audit that runs on CI. It ensures the library stays in sync with the Google Gemini API by:

-   Detecting new API methods and schema fields
-   Verifying that claimed implementations are accurate
-   Tracking missing features via GitHub issues

When adding new features or the API changes, you may need to update `integration/src/test/resources/compliance_config.json`.

**See the full documentation**: [API Compliance Audit](api-compliance.md)

## Code of Conduct

Please note that this project is released with a Contributor Code of Conduct. By participating in this project you agree to abide by its terms.
