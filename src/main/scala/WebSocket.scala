import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest, BinaryMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import spray.json._
import scala.concurrent.{ExecutionContextExecutor, Future}
import LuckyNumbersGame._
import scala.util.{Failure, Success}


// Data structures
case class PlayMessage(players: Int)
case class Result(position: Int, player: String, number: Int, result: Int)
case class ResultsMessage(results: List[Result])

case class PingMessage(id: Int, timestamp: Long) {
  def toJson: JsValue = {
    val jsonMap = Map(
      "message_type" -> JsString("request.ping"),
      "id" -> JsNumber(id),
      "timestamp" -> JsNumber(timestamp)
    )
    JsObject(jsonMap)
  }
}

case class PongMessage(requestId: Int, requestAt: Long, timestamp: Long) {
  def toJson: JsValue = {
    val jsonMap = Map(
      "message_type" -> JsString("response.pong"),
      "request_id" -> JsNumber(requestId),
      "request_at" -> JsNumber(requestAt),
      "timestamp" -> JsNumber(timestamp)
    )
    JsObject(jsonMap)
  }
}

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

  // Define constants
  val PingMessageType = "request.ping"
  val PlayMessageType = "request.play"
  val PongMessageType = "response.pong"

  def websocketFlow: Flow[Message, Message, NotUsed] = {
    Flow[Message]
      .mapConcat {
        case TextMessage.Strict(text) =>
          text.parseJson match {
            case JsObject(fields) =>
              fields("message_type") match {
                case JsString(PlayMessageType) =>
                  val playMessage = text.parseJson.convertTo[PlayMessage]
                  // TODO: Handle play request
                  Nil
                case JsString(PingMessageType) =>
                  val pingMessage = text.parseJson.convertTo[PingMessage]
                  val pongMessage = PongMessage(pingMessage.id, pingMessage.timestamp, System.currentTimeMillis())
                  println(s"Pong message data: $pongMessage")
                  List(TextMessage(pongMessage.toJson.compactPrint))
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
  server.onComplete {
    case Success(_) =>
      println("Successfully started on localhost:8080 ")
    case Failure(ex) =>
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


  val websocketFlowClient = Http().webSocketClientFlow(WebSocketRequest("ws://localhost:8080/game"))

  val pingRequest = PingMessage(id = 1, System.currentTimeMillis())
  val sendPingSource: Source[Message, NotUsed] = Source.single(TextMessage(pingRequest.toJson.compactPrint))
  val pingFlow = sendPingSource.via(websocketFlowClient).to(printSink)
  pingFlow.run()
}