package sync3k.routes

import akka.http.scaladsl.testkit.{ ScalatestRouteTest, WSProbe }
import org.scalatest.{ Matchers, WordSpec }

class WebSocketRoutesSpec extends WordSpec with Matchers with ScalatestRouteTest with WebSocketRoutes {
  // TODO(yiinho): embed kafka.
  override implicit var kafkaServer: String = _

  "In-memory endpoint" should {
    "echo updates" in {
      val wsClient = WSProbe()

      WS("/ws/0", wsClient.flow) ~> webSocketRoutes ~> check {
        isWebSocketUpgrade shouldBe true

        wsClient.sendMessage("test1")
        wsClient.expectMessage("""{"id":0,"message":"test1"}""")

        wsClient.sendMessage("test2")
        wsClient.expectMessage("""{"id":1,"message":"test2"}""")

        wsClient.sendCompletion()
        wsClient.expectCompletion()
      }
    }

    "replay updates" in {
      val wsClient2 = WSProbe()

      WS("/ws/0", wsClient2.flow) ~> webSocketRoutes ~> check {
        isWebSocketUpgrade shouldBe true

        wsClient2.expectMessage("""{"id":0,"message":"test1"}""")
        wsClient2.expectMessage("""{"id":1,"message":"test2"}""")
        wsClient2.sendCompletion()
        wsClient2.expectCompletion()
      }
    }

    "skip to offset" in {
      val wsClient3 = WSProbe()

      WS("/ws/1", wsClient3.flow) ~> webSocketRoutes ~> check {
        isWebSocketUpgrade shouldBe true

        wsClient3.expectMessage("""{"id":1,"message":"test2"}""")
        wsClient3.sendCompletion()
        wsClient3.expectCompletion()
      }
    }
  }
}
