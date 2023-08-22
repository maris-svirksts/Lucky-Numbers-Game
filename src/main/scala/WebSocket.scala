import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, BinaryMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import spray.json._
import scala.concurrent.{ExecutionContextExecutor, Future, Await}
import LuckyNumbersGame._
import scala.util.{Failure, Success}
import org.slf4j.LoggerFactory
import scala.concurrent.duration._

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
  implicit val playFormat = jsonFormat1(PlayMessage)
  implicit val resultFormat = jsonFormat4(Result)
  implicit val resultsFormat = jsonFormat1(ResultsMessage)
  implicit val pongFormat = jsonFormat(
    { (request_id: Int, request_at: Long, timestamp: Long) => PongMessage(request_id, request_at, timestamp) },
    "request_id", "request_at", "timestamp"
  )
}

import JsonFormats._

object WebSocketServer extends App {
  implicit val system: ActorSystem = ActorSystem("WebSocketServer")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  private lazy val logger = LoggerFactory.getLogger(getClass)

  def websocketFlow: Flow[Message, Message, NotUsed] = {
    // Define constants
    val PingMessageType = "request.ping"
    val PlayMessageType = "request.play"

    Flow[Message].mapAsync(1) {
      case TextMessage.Strict(text) =>
        try {
          val json = text.parseJson.asJsObject
          json.fields.getOrElse("message_type", JsString("")).convertTo[String] match {
            case `PlayMessageType` =>
              val playMessage = json.convertTo[PlayMessage]
              logger.info(s"Play message data: $playMessage")

              val playResultsFuture = LuckyNumbersGame.play(playMessage.players)

              playResultsFuture.onComplete {
                case Success(results) => logger.info(s"Play results: $results")
                case Failure(exception) => logger.error(s"Failed to get play results", exception)
              }

              playResultsFuture.map { results =>
                val resultsMessage = ResultsMessage(
                  results.map(r => Result(r.position, r.player.id.toString, r.player.luckyNumber, r.result)).toList
                )

                logger.info(s"Transformed results message: $resultsMessage")

                val responseJson = JsObject(
                  Map(
                    "message_type" -> JsString("response.results"),
                    "results" -> resultsMessage.toJson
                  )
                )

                logger.info(s"JSON response: ${responseJson.compactPrint}")
                TextMessage(responseJson.compactPrint)
              }.recover {
                case e: Exception =>
                  logger.error(s"Error processing PlayMessageType: ", e)
                  TextMessage("{}")  // Send back an empty JSON as a failure response
              }
            case `PingMessageType` =>
              val pingMessage = json.convertTo[PingMessage]
              val pongMessage = PongMessage(pingMessage.id, pingMessage.timestamp, System.currentTimeMillis())
              logger.info(s"Pong message data: $pongMessage")
              Future.successful(TextMessage(pongMessage.toJson.compactPrint))
            case _ =>
              logger.warn(s"Unknown message type: $text")
              Future.successful(TextMessage("""{"message_type": "response.error", "message": "Unknown message type"}"""))  // Enhanced error message
          }
        } catch {
          case e: spray.json.JsonParser.ParsingException =>
            logger.error(s"Could not parse message: $text", e)
            Future.successful(TextMessage("""{"message_type": "response.error", "message": "Invalid JSON format"}"""))  // Enhanced error message
        }
      case _: TextMessage.Streamed =>
        logger.warn("Received a Streamed TextMessage, ignoring it.")
        Future.successful(TextMessage("""{"message_type": "response.error", "message": "Streamed TextMessage not supported"}"""))

      case _: BinaryMessage =>
        logger.warn("Received a BinaryMessage, ignoring it.")
        Future.successful(TextMessage("""{"message_type": "response.error", "message": "BinaryMessage not supported"}"""))
    }
    .recover {
      case e: Exception =>
        logger.error(s"Unexpected error processing message", e)
        TextMessage(s"""{"message_type": "response.error", "message": "$e"}""")
    }
  }

  val route = path("game") {
    handleWebSocketMessages(websocketFlow)
  }

  val server = Http().newServerAt("localhost", 8080).bind(route)

  server.onComplete {
    case Success(_) =>
      logger.info("Successfully started on localhost:8080")
    case Failure(ex) =>
      logger.error(s"Failed to start the server due to: ${ex.getMessage}", ex)
  }

  // Trap termination signals to trigger shutdown
  scala.sys.addShutdownHook {
    logger.info("Shutting down server...")
    
    server.flatMap(_.unbind()).onComplete {
      case Success(_) => logger.info("Server unbound.")
      case Failure(e) => logger.error(s"Error unbinding the server: ${e.getMessage}", e)
    }
    
    val termination = system.terminate()
    termination.onComplete {
      case Success(_) => logger.info("Server shutdown complete.")
      case Failure(e) => logger.error(s"Error during system termination: ${e.getMessage}", e)
    }
  }
}
