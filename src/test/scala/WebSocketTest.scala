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

  // Define your WebSocket route
  val route: Route = path("game") {
    handleWebSocketMessages(WebSocketServer.websocketFlow)
  }
  val logger = LoggerFactory.getLogger(getClass)

  "The Web Server / PingPong" must {
    "respond with a pong message when receiving a ping message" in {
      // Create WebSocket client probe
      val wsClient = WSProbe()

      // WS creates a WebSocket request for testing
      WS("/game", wsClient.flow) ~> route ~>
      check {
          val pingMessage = PingMessage(id = 1, System.currentTimeMillis())
          val messageToSend = TextMessage(pingMessage.toJson.compactPrint)

          // Logging the sent message
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
    "return correct results on Play request" in {
      logger.info("Testing Play request...")

      // Create WS probe
      val wsClient = WSProbe()

      // Start WS conversation
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

        // Check response
        wsClient.expectMessage() match {
          case TextMessage.Strict(text) =>
            val jsonResponse = text.parseJson.asJsObject
            jsonResponse.fields.get("message_type") match {
              case Some(JsString("response.results")) => 
                val results = jsonResponse.fields("results").convertTo[List[Result]]
                results should have size 3  // since we have 3 players
                results.map(_.position) shouldBe List(1, 2, 3)  // should have distinct positions

                logger.debug(s"Received response: $text")

              case Some(other) => 
                fail(s"Unexpected message_type value: $other")

              case None => 
                fail("message_type key not found in the response")
            }

          case _ =>
            fail("Received unexpected message type")
        }
      }
    }

    "return empty results for zero players" in {
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
          case Some(JsString("response.results")) => 
            jsonResponse.fields("results").convertTo[List[Result]] shouldBe empty

          case Some(other) => 
            fail(s"Unexpected message_type value: $other")

          case None => 
            fail("message_type key not found in the response")
        }
      }
    }
  }
}
