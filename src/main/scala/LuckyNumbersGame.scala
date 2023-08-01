import scala.util.Random
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global

object LuckyNumbersGame {

  // Define Player case class
  case class Player(id: Int, luckyNumber: Int)

  // Define Results case class
  case class Results(player: Player, result: Int, position: Int = 0)

  // Function to generate a random number
  def generateRandomNumber(): Int = Random.nextInt(1000000)

  // Function to calculate a result based on a lucky number
  def calculateResult(luckyNumber: Int): Int = {
    // Count the occurrence of each digit in the lucky number
    val occurrences = luckyNumber.toString.groupBy(identity).mapValues(_.length)

    // For each digit, calculate the result as 10^(occurrences-1) * digit and sum them all up
    occurrences.map {
      case (digit, counter) => Math.pow(10, counter - 1).toInt * digit.asDigit
    }.sum
  }

  // Function to add players and their corresponding results
  def addPlayersWithResults(numberOfPlayers: Int): Future[Seq[Results]] = Future.sequence {
    // For each player, generate a random lucky number and calculate the result
    // Wrap the calculation inside a Future and return a sequence of Futures
    (1 to numberOfPlayers).map { playerId =>
      Future {
        val player = Player(playerId, generateRandomNumber())
        val result = calculateResult(player.luckyNumber)
        Results(player, result)
      }
    }
  }

  // Function to play the game with a specific number of players
  def play(numberOfPlayers: Int): Future[Seq[Results]] = {
    // Using a for-comprehension to handle the Futures
    for {
      // Await the results of the players
      results <- addPlayersWithResults(numberOfPlayers)
      
      // Await the result of the bot player
      botResult <- addPlayersWithResults(1).map(_.head.result)

      // Filter out the winners who have a result greater than the bot's result
      winners = results.filter(_.result > botResult)

      // Sort the winners by their results in descending order
      sortedResults = winners.sortBy(-_.result)
    } yield {
      // Assign positions to the winners and return the results
      sortedResults.zipWithIndex.map { case (result, index) =>
        result.copy(position = index + 1)
      }
    }
  }
}
