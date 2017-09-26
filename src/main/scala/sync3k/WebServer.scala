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

package sync3k

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import sync3k.routes.{ BaseRoutes, SimpleRoutes, WebSocketRoutes }

object WebServer extends Directives with SimpleRoutes with WebSocketRoutes {
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit var kafkaServer: String = _

  def main(args: Array[String]) {

    case class Config(bind: String = "0.0.0.0", port: Int = 8080, kafkaServer: String = "localhost:9092")

    val parser = new scopt.OptionParser[Config]("sync3k-server") {
      head("sync3k-server")

      opt[String]('b', "bind")
        .action((x, c) => c.copy(bind = x))
        .text("interface to bind to. Defaults to 0.0.0.0")

      opt[Int]('p', "port")
        .action((x, c) => c.copy(port = x))
        .text("port number to listen to. Defaults to 8080")

      opt[String]('k', "kafkaServer")
        .action((x, c) => c.copy(kafkaServer = x))
        .text("Kafka bootstrap server. Defaults to localhost:9092")
    }

    val config = parser.parse(args, Config())

    if (config.isEmpty) {
      system.terminate()
      return
    }

    kafkaServer = config.get.kafkaServer

    // needed for the future flatMap/onComplete in the end

    val bindingFuture = Http().bindAndHandle(routes, config.get.bind, config.get.port)

    println(s"Server online at http://localhost:${config.get.port}/")

    scala.sys.addShutdownHook {
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate()) // and shutdown when done
    }
  }

  val routes = BaseRoutes.baseRoutes ~ simpleRoutes ~ webSocketRoutes
}
