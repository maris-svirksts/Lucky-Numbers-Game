import scala.util.Random
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global

object LuckyNumbersGame {

  val MaxRandomNumber: Int = 1000000

  case class Player(id: Int, luckyNumber: Int)

  case class Results(player: Player, result: Int, position: Int = 0)

  def generateRandomNumber(): Int = Random.nextInt(MaxRandomNumber)

  /**
   * Calculate a result based on a lucky number.
   * For each digit in the lucky number, calculate the result as 10^(occurrences-1) * digit and sum them all up.
   */
  def calculateResult(luckyNumber: Int): Int = {
    val occurrences = luckyNumber.toString.groupBy(identity).mapValues(_.length)
    occurrences.map {
      case (digit, counter) => Math.pow(10, counter - 1).toInt * digit.asDigit
    }.sum
  }

  /**
   * Add players and their corresponding results.
   * For each player, generate a random lucky number and calculate the result.
   */
  def addPlayersWithResults(numberOfPlayers: Int): Future[Seq[Results]] = {
    require(numberOfPlayers > 0, "Number of players must be greater than 0")
    Future.traverse((1 to numberOfPlayers).toList) { playerId =>
      Future {
        val player = Player(playerId, generateRandomNumber())
        val result = calculateResult(player.luckyNumber)
        Results(player, result)
      }
    }
  }

  /**
   * Play the game with a specific number of players.
   * Filter out the winners who have a result greater than the bot's result.
   * Assign positions to the winners and return the results.
   */
  def play(numberOfPlayers: Int): Future[Seq[Results]] = {
    require(numberOfPlayers > 0, "Number of players must be greater than 0")
    for {
      results <- addPlayersWithResults(numberOfPlayers)
      botResult <- addPlayersWithResults(1).map(_.head.result)
    } yield {
      val winners = results.filter(_.result > botResult)
      val sortedWinners = winners.sortBy(-_.result)
      sortedWinners.zipWithIndex.map { case (result, index) => result.copy(position = index + 1) }
    }
  }
}
