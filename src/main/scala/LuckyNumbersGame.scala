import scala.util.{Random}
import scala.concurrent.{Future}
import scala.concurrent.ExecutionContext.Implicits.global
import org.slf4j.LoggerFactory

object LuckyNumbersGame {

  val MaxRandomNumber: Int = 1000000
  private lazy val logger = LoggerFactory.getLogger(getClass)

  case class Player(id: Int, luckyNumber: Int)
  case class Results(player: Player, result: Int, position: Int = 0)

  def generateRandomNumber(): Int = Random.nextInt(MaxRandomNumber)

  /**
   * Calculate a result based on a lucky number.
   * For each digit in the lucky number, calculate the result as 10^(occurrences-1) * digit and sum them all up.
   */
  def calculateResult(luckyNumber: Int): Int = {
    Option(luckyNumber).map { num =>
      val occurrences = num.toString.groupBy(identity).mapValues(_.length)
      occurrences.map {
        case (digit, counter) => Math.pow(10, counter - 1).toInt * digit.asDigit
      }.sum
    }.getOrElse(0)
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
      }.recover {
        case e: Throwable => 
          logger.error(s"Failed to generate results for player $playerId", e)
          Results(Player(playerId, 0), 0)
      }
    }
  }

  /**
   * Filter winners based on bot's result, sort them by their result in descending order
   * and assign them positions.
   */
  private def determineWinners(results: Seq[Results], botResult: Int): Seq[Results] = {
    val winners = results.filter(_.result > botResult)
    logger.info(s"Winners filtered: $winners")
    
    val sortedWinners = winners.sortBy(-_.result)
    logger.info(s"Winners sorted: $sortedWinners")
    
    sortedWinners.zipWithIndex.map { case (result, index) => result.copy(position = index + 1) }
  }

  /**
   * Play the game with a specific number of players.
   */
  def play(numberOfPlayers: Int): Future[Seq[Results]] = {
    require(numberOfPlayers > 0, "Number of players must be greater than 0")
    logger.info(s"Starting play function for $numberOfPlayers players")

    for {
      results <- addPlayersWithResults(numberOfPlayers)
      _ = logger.info(s"Results for players obtained: $results")
      botResult <- addPlayersWithResults(1).map(_.headOption.map(_.result).getOrElse(0))
      _ = logger.info(s"Bot result obtained: $botResult")
      finalResults = determineWinners(results, botResult)
      _ = logger.info(s"Final results: $finalResults")
    } yield finalResults
  }.recover {
    case e: Throwable => 
      logger.error("Error during the game play", e)
      Seq.empty[Results]
  }
}
