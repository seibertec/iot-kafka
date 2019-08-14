package ch.seibertec.iot

import java.time.LocalDateTime

import akka.event.Logging
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Route

class WebRoute {

  import akka.http.scaladsl.server.Directives._

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
            complete(
              s""" { "temperature": 23.567, "tempUnit": "°C", "humidity": 80.3, "timestamp": "${LocalDateTime
                .now()}"} """)
          } ~
          path("timeseries") {
            parameter('scope) {
              case "last 12 month" =>
                complete(
                  s"""[
                        {  "temperature": 21, "tempUnit": "°C", "humidity": 80.3, "timestamp": "${LocalDateTime.now()}"},
                        {  "temperature": 21.5, "tempUnit": "°C", "humidity": 79.3, "timestamp": "${LocalDateTime.now()}"},
                        {  "temperature": 22, "tempUnit": "°C", "humidity": 78.3, "timestamp": "${LocalDateTime.now()}"},
                        {  "temperature": 23, "tempUnit": "°C", "humidity": 77.3, "timestamp": "${LocalDateTime.now()}"},
                        {  "temperature": 24.567, "tempUnit": "°C", "humidity": 76.3, "timestamp": "${LocalDateTime.now()}"},
                        {  "temperature": 25.567, "tempUnit": "°C", "humidity": 75.3, "timestamp": "${LocalDateTime.now()}"},
                        {  "temperature": 26.567, "tempUnit": "°C", "humidity": 74.3, "timestamp": "${LocalDateTime.now()}"}
                      ]"""
                )
              case x =>
                complete(StatusCodes.BadRequest, s"scope '$x' not supported!")
            }
          }
        }
    }
}
