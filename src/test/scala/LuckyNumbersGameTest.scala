import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

class LuckyNumbersGameTest extends AsyncFunSuite with Matchers {

  test("generateRandomNumber should generate numbers within the range 0 to 1000000") {
    val randomNumber = LuckyNumbersGame.generateRandomNumber()
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
}
