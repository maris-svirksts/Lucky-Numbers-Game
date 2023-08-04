# Lucky Numbers Game

This project is a simple game implemented using Akka Framework and Scala.

NOTE: This is NOT a full solution, I'll continue playing around with it. Publishing it in this state since I promised Anna Orlova that I'll get something done for her this week. And, as I already told Stanislav Kursish, my Scala is rusty, haven't worked on anything in two years, so, the amount of time I had available this week was not enough to finish everything.

## TODO

- Write tests for WebSocket functionality.
- Implement `request.play` functionality.
- Implement `response.results` functionality.
- Finish `request.ping` / `response.pong` functionality.
- Provide a list of winners based on their game results.
- Clean up and polish the overal results: divide into multiple files, add comments etc.


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
3. The application will start on `localhost:8080`.

## How to Test

1. Run `sbt test` in the terminal.
