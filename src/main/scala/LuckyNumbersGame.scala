import scala.util.Random
import scala.collection.parallel.ParSeq

object LuckyNumbersGame {
    case class Player(id: Int, luckyNumber: Int)
    case class Results(player: Player, result: Int)

    def generateRandomNumber(): Int = Random.nextInt(1000000)
    def calculateResult(luckyNumber: Int): Int = {
        val occurrences = luckyNumber.toString.groupBy(identity).mapValues(_.length)

        occurrences.map {
            case (digit, counter) => Math.pow(10, counter - 1).toInt * digit.asDigit
        }.sum
    }
    def addPlayers(numberOfPlayers: Int): ParSeq[Player] = {
        (1 to numberOfPlayers).map{
            playerId => Player(playerId, generateRandomNumber())
        }
    }
}