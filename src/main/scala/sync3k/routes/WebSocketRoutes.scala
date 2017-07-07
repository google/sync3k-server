package sync3k.routes

import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.IntNumber
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.WebSocketDirectives
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.collection.mutable.ObservableBuffer
import scala.collection.mutable.ArrayBuffer

trait WebSocketRoutes extends WebSocketDirectives {
  implicit val materializer: Materializer
  //val items: ObservableBuffer[String] = new ArrayBuffer[String] with ObservableBuffer[String] {}
  val items = ArrayBuffer[String]()

  def greeter(n: Int): Flow[Message, Message, Any] =
    Flow[Message].mapConcat {
      case tm: TextMessage =>
        tm.textStream.runForeach((item) => {
          items += item
        })
        items.drop(n).toList.map((item) => TextMessage(item))
      case bm: BinaryMessage =>
        bm.dataStream.runWith(Sink.ignore)
        Nil
    }

  def wssup: Flow[Message, Message, Any] =
    Flow.fromSinkAndSource[Message, Message](Sink.foreach({
      case tm: TextMessage =>
        tm.textStream.runForeach((item) => {
          items += item
        })
    }), Source
        .fromIterator(() => items.iterator.map((item) => TextMessage(item)))
        //.concat(Source.fromPublisher(items).map((item) => TextMessage(item)))
    )

  lazy val webSocketRoutes: Route =
    path("ws" / IntNumber) {
      (n) => handleWebSocketMessages(greeter(n))
    } ~ path("wssup") {
      handleWebSocketMessages(wssup)
    }
}
