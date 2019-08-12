package ch.seibertec.iot

import java.time.{LocalDateTime, LocalTime}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

object Main extends App {
  implicit val actorSystem: ActorSystem = ActorSystem("lcm-service")
  implicit val materializer = ActorMaterializer()

  println("Hello World!")

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