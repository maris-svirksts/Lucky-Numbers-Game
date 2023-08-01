# Lucky Numbers Game

This project is a simple game implemented using Play Framework and Scala.

## Overview

The game includes the following functionality:

- Generates a random number for each player and a bot player.
- Counts occurrences of each digit in the given number and calculates a game result for each player.
- Provides a list of winners based on their game results.

The game uses WebSocket communication for handling game logic:

- `request.play` message that initiates the game.
- `response.results` message that provides the game results.
- `request.ping` / `response.pong` messages for latency check.

## How to Run

1. Ensure you have sbt and Scala installed.
2. Run `sbt run` in the terminal.
3. The application will start on `localhost:9000`.

## How to Test

1. Run `sbt test` in the terminal.