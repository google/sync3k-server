package sync3k.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.IntNumber
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.WebSocketDirectives
import akka.kafka.scaladsl.{ Consumer, Producer }
import akka.kafka.{ ConsumerSettings, ProducerSettings, Subscriptions }
import akka.stream.Materializer
import akka.stream.scaladsl.{ BroadcastHub, Flow, Keep, MergeHub, Source }
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{ ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer }
import spray.json._

import scala.collection.mutable.ArrayBuffer

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

  case class OrderedMessage(id: Long, message: String)
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
        .filterNot(_._2 < skip)
        .map({ case (item, id) => TextMessage(OrderedMessage(id, item).toJson.toString) })
    // .keepAlive(30.seconds, () => TextMessage("\"no-op\""))
    )

  implicit val system: ActorSystem

  private lazy val producerSettings = ProducerSettings(
    system,
    new ByteArraySerializer,
    new StringSerializer
  ).withBootstrapServers("localhost:9092")

  private lazy val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("group1")

  def wsKafka(topic: String, offset: Long = 0): Flow[Message, Message, Any] =
    Flow.fromSinkAndSource[Message, Message](
      Flow[Message].flatMapConcat({
        case tm: TextMessage =>
          tm.textStream
      })
        .map((item) => new ProducerRecord[Array[Byte], String](topic, item))
        .to(Producer.plainSink(producerSettings)),
      Consumer.plainSource(consumerSettings, Subscriptions.assignmentWithOffset(
        new TopicPartition(topic, 0) -> offset
      ))
        .map((message) => {
          TextMessage(OrderedMessage(message.offset(), message.value()).toJson.toString())
        })
    )

  lazy val webSocketRoutes: Route = {
    pathPrefix("ws") {
      pathEndOrSingleSlash {
        handleWebSocketMessages(wssup())
      } ~ path(IntNumber) {
        (n) => handleWebSocketMessages(wssup(n))
      }
    } ~ path("kafka" / Segment / LongNumber) {
      (topic, n) => handleWebSocketMessages(wsKafka(topic, n))
    }
  }
}
