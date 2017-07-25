package sync3k

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import sync3k.routes.{ BaseRoutes, SimpleRoutes, WebSocketRoutes }

import scala.io.StdIn

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

    println(s"Server online at http://localhost:${config.get.port}/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

  val routes = BaseRoutes.baseRoutes ~ simpleRoutes ~ webSocketRoutes
}
