import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{TextMessage, Message}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import akka.event.Logging
import spray.json._
import JsonFormats._


class WebSocketServerSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  // Define your WebSocket route
  val route: Route = path("game") {
    handleWebSocketMessages(WebSocketServer.websocketFlow)
  }

  "The WebSocketServer" must {
    "respond with a pong message when receiving a ping message" in {
      // Create WebSocket client probe
      val wsClient = WSProbe()

      // WS creates a WebSocket request for testing
        WS("/game", wsClient.flow) ~> route ~>
        check {
            val pingMessage = PingMessage(id = 1, System.currentTimeMillis())
            val messageToSend = TextMessage(pingMessage.toJson.compactPrint)

            // Logging the sent message
            val log = Logging(system, this.getClass)
            log.info(s"Sending message: $messageToSend")

            wsClient.sendMessage(messageToSend)
            wsClient.expectMessage() match {
            case TextMessage.Strict(text) =>
                log.info(s"Received message: $text")
                val receivedPong = text.parseJson.convertTo[PongMessage]

                receivedPong.requestId shouldBe pingMessage.id

            case other =>
                fail(s"Unexpected message type received: $other")
            }
        }

    }
  }
}
