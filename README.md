# Lucky Numbers Game

**Lucky Numbers Game** is a competitive game where players and a bot each receive a random number. The game then counts occurrences of each digit in the number and determines a result for each participant. The primary objective is to compete against the bot and other players to achieve the best score. This project leverages the **Akka Framework** and **Scala** for seamless backend operations and real-time updates.

## Overview

The game comprises the following features:

- **Random Number Generation**: Each participant, including a bot player, gets a unique number.
  
- **Score Calculation**: The game evaluates occurrences of each digit in the assigned number and computes a score for each player.
  
- **Winner Declaration**: Based on individual scores, the game provides a list of winners.

For real-time interaction and updates, the game utilizes **WebSocket** communication. For those unfamiliar with WebSockets, they offer a persistent connection between the client and server, enabling real-time data exchanges. You can read more about WebSockets [here](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API). 

The WebSocket communication handles the game logic through the following messages:

- `request.play`: Initiates the game.
- `response.results`: Provides the game results.
- `request.ping` / `response.pong`: Used for latency checks.

## Getting Started

### Prerequisites

Ensure you have the following software installed:
- **sbt**
- **Scala**

If you don't have them installed, follow the installation guide for [sbt](https://www.scala-sbt.org/download.html) and [Scala](https://scala-lang.org/download/).

### How to Run

1. Open your terminal.
2. Navigate to the project directory.
3. Enter the command `sbt run`.

### How to Test

#### Running Tests

To run all tests:
1. Open your terminal.
2. Navigate to the project directory.
3. Enter the command `sbt test`.

For running specific tests, use the `test-only` command followed by the test class name.

#### Code Coverage

This project includes code coverage tests through [sbt-scoverage](https://github.com/scoverage/sbt-scoverage). To generate a code coverage report, follow these steps:

1. Open your terminal.
2. Navigate to the project directory.
3. Enter the command `sbt coverage test`.
4. After tests complete, you can generate the coverage report by running `sbt coverageReport`.

The code coverage reports will be generated in the `<project-directory>/target/scala-2.xx/scoverage-report/` directory.

The minimum code coverage is set to 80%.

## TODO

- Improve existing tests: separate setup and logic better.
- Write additional tests.
- Clean up and polish the overall results: divide into multiple files, add comments, etc.
