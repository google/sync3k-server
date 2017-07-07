package sync3k.routes

import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.IntNumber
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.WebSocketDirectives
import akka.stream.Materializer
import akka.stream.scaladsl.{ BroadcastHub, Flow, Keep, MergeHub, Source }

import spray.json._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

trait WebSocketRoutes extends WebSocketDirectives {
  implicit val materializer: Materializer
  private val items = ArrayBuffer[(String, Int)]()
  lazy val (sink, source) =
    MergeHub.source[String]
      .zip(Source(Stream.from(1)))
      .map((item) => {
        items += item; item
      })
      .toMat(BroadcastHub.sink)(Keep.both)
      .run()

  case class OrderedMessage(id: Int, message: String)
  object OrderedMessageProtocol extends DefaultJsonProtocol {
    implicit val orderedMessageFormat = jsonFormat2(OrderedMessage.apply)
  }

  import OrderedMessageProtocol._

  def wssup(skip: Int = 0): Flow[Message, Message, Any] =
    Flow.fromSinkAndSource[Message, Message](
      Flow[Message].flatMapConcat({
        case tm: TextMessage =>
          tm.textStream
      }).to(sink),
      Source(items.toList)
        .concat(source)
        .filterNot(_._2 <= skip)
        .map({ case (item, id) => TextMessage(OrderedMessage(id, item).toJson.toString) })
        .keepAlive(30.seconds, () => TextMessage("\"no-op\""))
    )

  lazy val webSocketRoutes: Route = {
    pathPrefix("ws") {
      pathEndOrSingleSlash {
        handleWebSocketMessages(wssup())
      } ~ path(IntNumber) {
        (n) => handleWebSocketMessages(wssup(n))
      }
    }
  }
}
