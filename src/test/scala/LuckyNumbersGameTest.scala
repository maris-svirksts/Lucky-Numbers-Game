import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

class LuckyNumbersGameTest extends AsyncFunSuite with Matchers {

  test("generateRandomNumber should generate numbers within the range 0 to 1000000") {
    val randomNumberGenerator = new LuckyNumbersGame.RandomNumberGenerator
    val randomNumber = randomNumberGenerator.generateNumber()
    randomNumber should be >= 0
    randomNumber should be < 1000000
  }

  test("calculateResult should correctly calculate results for known inputs") {
    LuckyNumbersGame.calculateResult(447974) shouldBe 479
    LuckyNumbersGame.calculateResult(11111) shouldBe 10000
    LuckyNumbersGame.calculateResult(99999) shouldBe 90000
  }

  test("addPlayersWithResults should return correct number of results") {
    LuckyNumbersGame.addPlayersWithResults(10).map { results =>
      results should have length 10
    }
  }

  test("play should return results with positions assigned") {
    LuckyNumbersGame.play(10).map { results =>
      results.zipWithIndex.forall { case (result, index) =>
        result.position == index + 1
      } shouldBe true
    }
  }

  test("play should return results sorted by result") {
    LuckyNumbersGame.play(10).map { results =>
      val resultsSortedByResult = results.sortBy(-_.result)
      results shouldEqual resultsSortedByResult
    }
  }

  test("play with predefined numbers should return correct results and positions") {
    val predefinedNumbersForPlayers = Seq(111111, 222222, 333333, 444444, 555555)
    val predefinedNumberForBot = Seq(111111)
    
    val playersNumberGenerator = new LuckyNumbersGame.PredefinedNumberGenerator(predefinedNumbersForPlayers.iterator)
    val botNumberGenerator = new LuckyNumbersGame.PredefinedNumberGenerator(predefinedNumberForBot.iterator)
    
    // Start the game with predefined numbers for players and bot
    LuckyNumbersGame.play(5, playersNumberGenerator, botNumberGenerator).map { results =>
      
      // Verify the positions assigned based on the predefined lucky numbers
      results should have length 4
      
      results(0).result shouldBe 500000
      results(0).position shouldBe 1
      
      results(1).result shouldBe 400000
      results(1).position shouldBe 2

      results(2).result shouldBe 300000
      results(2).position shouldBe 3

      results(3).result shouldBe 200000
      results(3).position shouldBe 4
    }
  }
}
