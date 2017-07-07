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

  def main(args: Array[String]) {

    // needed for the future flatMap/onComplete in the end

    val bindingFuture = Http().bindAndHandle(routes, "0.0.0.0", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

  // Here you can define all the different routes you want to have served by this web server
  // Note that routes might be defined in separated traits like the current case
  val routes = BaseRoutes.baseRoutes ~ simpleRoutes ~ webSocketRoutes

}
