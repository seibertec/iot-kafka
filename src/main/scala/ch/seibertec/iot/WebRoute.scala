package ch.seibertec.iot

import java.time.LocalDateTime

import akka.actor.ActorRef
import akka.pattern.ask
import akka.event.Logging
import akka.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  StatusCode,
  StatusCodes
}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import java.util.concurrent.TimeUnit._

import ch.seibertec.iot.consumer.IotEventListener.{
  Last12Month,
  LatestValue,
  NoData
}
import ch.seibertec.iot.domain.{SensorData, SensorDataMessage}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import scala.util.Success

class WebRoute(iotEventListener: ActorRef) extends PlayJsonSupport {

  import akka.http.scaladsl.server.Directives._
  import SensorDataMessage._

  implicit val timeout = Timeout(20, SECONDS)

  def route: Route =
    logRequestResult(("/", Logging.InfoLevel)) {
      path("hello") {
        get {
          complete(
            HttpEntity(ContentTypes.`text/html(UTF-8)`,
                       "<h1>Say hello to akka-http</h1>"))
        }
      } ~
        pathPrefix("static") {
          getFromResourceDirectory("static")
        } ~
        pathPrefix("data") {
          path("latest") {
            parameter('sensorName) { sensorName =>
              onSuccess(iotEventListener ? LatestValue(sensorName)) {
                case NoData =>
                  complete(StatusCodes.NotFound)
                case s: SensorDataMessage =>
                  complete(s)
              }
            }
          } ~
            path("timeseries") {
              parameter('scope, 'sensorName) { (scope, sensorName) =>
                onSuccess((iotEventListener ? Last12Month(scope, sensorName))
                  .mapTo[List[SensorDataMessage]]) { res =>
                  complete(res)
                }
              }
            }
        }
    }
}
