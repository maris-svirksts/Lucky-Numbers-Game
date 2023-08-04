import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest, BinaryMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import spray.json._
import scala.concurrent.{ExecutionContextExecutor, Future}


// Data structures
case class PingMessage(id: Int, timestamp: Long)
case class PongMessage(requestId: Int, requestAt: Long, timestamp: Long)
case class PlayMessage(players: Int)
case class Result(position: Int, player: String, number: Int, result: Int)
case class ResultsMessage(results: List[Result])

// Json formats
object JsonFormats extends DefaultJsonProtocol {
  implicit val pingFormat = jsonFormat2(PingMessage)
  implicit val pongFormat = jsonFormat3(PongMessage)
  implicit val playFormat = jsonFormat1(PlayMessage)
  implicit val resultFormat = jsonFormat4(Result)
  implicit val resultsFormat = jsonFormat1(ResultsMessage)
}

import JsonFormats._

object WebSocketServer extends App {
  implicit val system: ActorSystem = ActorSystem("WebSocketServer")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // WebSocket handler
  def websocketFlow: Flow[Message, Message, NotUsed] = {
    Flow[Message]
      .mapConcat {
        case TextMessage.Strict(text) =>
          text.parseJson match {
            case JsObject(fields) =>
              fields("message_type") match {
                case JsString("request.play") =>
                  val playMessage = text.parseJson.convertTo[PlayMessage]
                  // TODO: Handle play request
                  Nil
                case JsString("request.ping") =>
                  val pingMessage = text.parseJson.convertTo[PingMessage]
                  List(TextMessage(PongMessage(pingMessage.id, pingMessage.timestamp, System.currentTimeMillis()).toJson.compactPrint))
                case _ =>
                  println(s"Unknown message type: $text")
                  Nil
              }
            case _ =>
              println(s"Could not parse message: $text")
              Nil
          }
        case _ =>
          Nil
      }
  }

  val route = path("game") {
    handleWebSocketMessages(websocketFlow)
  }

  val server = Http().newServerAt("localhost", 8080).bind(route)
  server.map { _ =>
    println("Successfully started on localhost:8080 ")
  } recover { case ex =>
    println("Failed to start the server due to: " + ex.getMessage)
  }

  val printSink: Sink[Message, Future[Done]] = Sink.foreach[Message] {
    case message: TextMessage.Strict =>
      println(message.text)
    case _: TextMessage.Streamed =>
      println("Received a Streamed TextMessage, ignoring it.")
    case _: BinaryMessage =>
      println("Received a BinaryMessage, ignoring it.")
  }


  val sendSource: Source[Message, NotUsed] = Source.single(TextMessage(PingMessage(1, System.currentTimeMillis()).toJson.compactPrint))

  val websocketFlowClient = Http().webSocketClientFlow(WebSocketRequest("ws://localhost:8080/game"))

  val flow = sendSource.via(websocketFlowClient).to(printSink)

  flow.run()

}
