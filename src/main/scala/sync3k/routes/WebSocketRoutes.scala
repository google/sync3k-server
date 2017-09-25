// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package sync3k.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.IntNumber
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.WebSocketDirectives
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Source}
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}
import spray.json._

import scala.collection.mutable.ArrayBuffer

trait WebSocketRoutes extends WebSocketDirectives {
  implicit val materializer: Materializer
  implicit var kafkaServer: String

  private val items = ArrayBuffer[(String, Int)]()
  lazy val (sink, source) =
    MergeHub.source[String]
      .zip(Source(Stream.from(0)))
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
    Flow.fromSinkAndSourceCoupled(
      Flow[Message].flatMapConcat({
        case tm: TextMessage =>
          tm.textStream
      }).to(sink),
      Source(items.toList)
        .concat(source)
        .filterNot(_._2 < skip)
        .map({ case (item, id) => TextMessage(OrderedMessage(id, item).toJson.toString) })
    )

  implicit val system: ActorSystem

  private lazy val producerSettings = ProducerSettings(
    system,
    new ByteArraySerializer,
    new StringSerializer
  ).withBootstrapServers(kafkaServer)
    .withProperty(ProducerConfig.ACKS_CONFIG, "all")
    .withProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy")

  private lazy val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers(kafkaServer)
    .withGroupId("group1")

  def wsKafka(topic: String, offset: Long = 0): Flow[Message, Message, Any] =
    Flow.fromSinkAndSourceCoupled[Message, Message](
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
