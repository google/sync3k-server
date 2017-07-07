package sync3k.routes

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.IntNumber
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.WebSocketDirectives
import akka.stream.Materializer
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Source}

import scala.collection.mutable.ArrayBuffer

trait WebSocketRoutes extends WebSocketDirectives {
  implicit val materializer: Materializer
  private val items = ArrayBuffer[(String, Int)]()
  lazy val (sink, source) =
    MergeHub.source[String]
      .zip(Source(Stream.from(1)))
      .toMat(BroadcastHub.sink)(Keep.both)
      .run()

  def wssup(skip: Int = 0): Flow[Message, Message, Any] =
    Flow.fromSinkAndSource[Message, Message](
      Flow[Message].flatMapConcat({
        case tm: TextMessage =>
          tm.textStream
      }).to(sink),
      Source(items.toList)
        .concat(source)
        .filterNot(_._2 <= skip)
        .map({ case (item, id) => TextMessage(s"$id: $item") })
    )

  lazy val webSocketRoutes: Route = {
    source.runForeach((item) => {
      items += item
    })
    pathPrefix("ws") {
      pathEndOrSingleSlash {
        handleWebSocketMessages(wssup())
      } ~ path(IntNumber) {
        (n) => handleWebSocketMessages(wssup(n))
      }
    }
  }
}
