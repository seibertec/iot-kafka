package ch.seibertec.iot

import java.time.{LocalDateTime, LocalTime}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import ch.seibertec.iot.config.{IotKafkaConfig, IotKafkaConfigAcessor}
import ch.seibertec.iot.consumer.IotEventListener
import com.typesafe.config.Config

object Main extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("lcm-service")
  implicit val materializer = ActorMaterializer()

  println("Hello World!")
  val configAccessor: IotKafkaConfigAcessor = IotKafkaConfig()
  val consumerConfig: Config = configAccessor.kafkaEventConsumer
  IotEventListener(consumerConfig, "sensortopic")
  val bindingFuture = Http().bindAndHandle(new WebRoute().route, "localhost", 8090)

}


class WebRoute {

  import akka.http.scaladsl.server.Directives._

  def route: Route =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    } ~
    pathPrefix("static") {
      getFromResourceDirectory("static")
    } ~
    pathPrefix("data") {
      path("last") {
        complete(s""" { "temperature": 23.567, "tempUnit": "°C", "humidity": 80.3, "timestamp": "${LocalDateTime.now()}"} """)
      }
    }

}