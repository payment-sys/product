# product

Spring Boot service extracted from the original ackend monorepo. This repository builds independently.

## Requirements

- JDK 21 (or Docker)
- Gradle is provided by the wrapper

## Build and test

`ash
./gradlew clean build
`

On Windows PowerShell:

`powershell
.\gradlew.bat clean build
`

## Run locally

Configure the required application environment variables (see src/main/resources/application.yaml), then run:

`ash
./gradlew bootRun
`

## Build Docker image

`ash
docker build -t product:local .
`

The GitHub Actions workflow runs the Gradle build and tests for pushes and pull requests to main.
