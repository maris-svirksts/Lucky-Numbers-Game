import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{TextMessage, Message}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._
import JsonFormats._
import org.slf4j.LoggerFactory

class WebSocketServerSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  // Define the WebSocket route
  val route: Route = path("game") {
    handleWebSocketMessages(WebSocketServer.websocketFlow)
  }

  val logger = LoggerFactory.getLogger(getClass)

  "The Web Server / PingPong" must {
    "respond with a pong message when receiving a ping message" in {
      // Create WebSocket client probe
      val wsClient = WSProbe()

      // Test setup for WebSocket communication
      WS("/game", wsClient.flow) ~> route ~>
      check {
          val pingMessage = PingMessage(id = 1, System.currentTimeMillis())
          val messageToSend = TextMessage(pingMessage.toJson.compactPrint)

          // Log the sent message
          logger.info(s"Sending message: $messageToSend")

          wsClient.sendMessage(messageToSend)
          wsClient.expectMessage() match {
          case TextMessage.Strict(text) =>
              logger.info(s"Received message: $text")
              val receivedPong = text.parseJson.convertTo[PongMessage]
              receivedPong.requestId shouldBe pingMessage.id

          case other =>
              fail(s"Unexpected message type received: $other")
          }
      }
    }
  }

  "The Web Server / Game" should {
    // Testing for correct results on Play request
    "return correct results on Play request" in {
      logger.info("Testing Play request...")

      // Create WebSocket client probe
      val wsClient = WSProbe()

      // Test setup for WebSocket communication
      WS("/game", wsClient.flow) ~> route ~>
      check {
        // Send Play request
        val playMessage = PlayMessage(3)
        val playMessageJson = JsObject(
          "message_type" -> JsString("request.play"),
          "players" -> JsNumber(playMessage.players)
        )

        wsClient.sendMessage(playMessageJson.compactPrint)
        logger.debug(s"Sent message: ${playMessageJson.compactPrint}")

        // Check response from the server
        wsClient.expectMessage() match {
          case TextMessage.Strict(text) =>
            val jsonResponse = text.parseJson.asJsObject
            jsonResponse.fields.get("message_type") match {
              case Some(JsString("response.results")) =>
                // Directly extract the results array from jsonResponse
                jsonResponse.fields.get("results") match {
                  case Some(JsArray(resultsArray)) =>
                    val results = resultsArray.toList.map(_.convertTo[Result])
                    results.size should (be >= 0 and be <= 3)  // there can be between 0 to 3 results

                    // Verify positions are distinct and in ascending order
                    results.map(_.position) shouldBe (1 to results.size).toList

                    logger.debug(s"Received response: $text")
                    
                  case None =>
                    fail("No results key found in the response.")
                  case _ =>
                    fail("Results key in the response was not an array.")
                }

              case Some(other) => 
                fail(s"Unexpected message_type value: $other")

              case None => 
                fail(s"message_type key not found in the response: $jsonResponse")
            }

          case _ =>
            fail("Received unexpected message type")
        }
      }
    }

    // Additional test for error handling
    "return an error for zero players" in {
      val wsClient = WSProbe()

      WS("/game", wsClient.flow) ~> route ~>
      check {
        val playMessage = PlayMessage(0)
        val playMessageJson = JsObject(
          "message_type" -> JsString("request.play"),
          "players" -> JsNumber(playMessage.players)
        )

        wsClient.sendMessage(playMessageJson.compactPrint)
        val response = wsClient.expectMessage()
        response.isText shouldBe true

        val jsonResponse = response.asTextMessage.getStrictText.parseJson.asJsObject
        jsonResponse.fields.get("message_type") match {
          case Some(JsString("response.error")) =>
            // Expect a specific error message for zero players
            val errorMessage = jsonResponse.fields("message").convertTo[String]
            errorMessage should include("Number of players must be greater than 0")

          case Some(other) => 
            fail(s"Unexpected message_type value: $other")

          case None => 
            fail(s"message_type key not found in the response: $jsonResponse")
        }
      }
    }
  }
}
