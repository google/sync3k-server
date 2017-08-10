package sync3k.routes

import akka.http.scaladsl.testkit.{ ScalatestRouteTest, WSProbe }
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }

class WebSocketRoutesSpec extends WordSpec with Matchers with ScalatestRouteTest with WebSocketRoutes with EmbeddedKafka with BeforeAndAfterAll {
  override implicit var kafkaServer: String = "localhost:6001"

  val baseRoots = Table(
    "base url",
    "/ws",
    "/kafka/test-1",
    "/kafka/test-2"
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    EmbeddedKafka.start()
  }

  override def afterAll(): Unit = {
    EmbeddedKafka.stop()
    super.afterAll()
  }

  forAll(baseRoots) { baseRoot =>
    baseRoot should {
      "echo updates" in {
        val wsClient = WSProbe()

        WS(s"$baseRoot/0", wsClient.flow) ~> webSocketRoutes ~> check {
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

        WS(s"$baseRoot/0", wsClient2.flow) ~> webSocketRoutes ~> check {
          isWebSocketUpgrade shouldBe true

          wsClient2.expectMessage("""{"id":0,"message":"test1"}""")
          wsClient2.expectMessage("""{"id":1,"message":"test2"}""")
          wsClient2.sendCompletion()
          wsClient2.expectCompletion()
        }
      }

      "skip to offset" in {
        val wsClient3 = WSProbe()

        WS(s"$baseRoot/1", wsClient3.flow) ~> webSocketRoutes ~> check {
          isWebSocketUpgrade shouldBe true

          wsClient3.expectMessage("""{"id":1,"message":"test2"}""")
          wsClient3.sendCompletion()
          wsClient3.expectCompletion()
        }
      }
    }
  }
}
