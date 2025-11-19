infra: setup auto-tagging and publishing on merge to main

## Description
This PR updates the CI configuration to automatically manage releases when changes are merged into the `main` branch.

### Changes
- **build.sbt**: Removed the hardcoded `version := "0.1.0-SNAPSHOT"`. The project now uses `sbt-dynver` to derive the version from git tags.
- **.github/workflows/ci.yml**:
    - Added `contents: write` permission to the workflow.
    - Replaced the `publish` job with a `release` job that runs only on pushes to `main`.
    - Implemented logic to:
        1. Calculate the next patch version based on the latest git tag.
        2. Create a new git tag.
        3. Publish artifacts to Maven Central using `sbt ci-release`.
        4. Push the new tag back to the repository.

## Verification
- Verified locally that `sbt version` correctly picks up the dynamic version (e.g., `0.0.0+...-SNAPSHOT`).
- The CI workflow has been updated to include these steps, which will execute upon merge.