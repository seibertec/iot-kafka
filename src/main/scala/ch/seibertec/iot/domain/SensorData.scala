package ch.seibertec.iot.domain

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import play.api.libs.json.{Format, JsPath}

case class SensorDataMessage(time: LocalDateTime,
                             data: SensorData,
                             unit: String)
case class SensorData(temperature: Float, humidity: Float)

import play.api.libs.functional.syntax._

object SensorDataMessage {
  private val formatterDateTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME

  implicit def toLocalDateTimeString(d: LocalDateTime): String =
    formatterDateTime.format(d)

  implicit def toLocalDateTime(dateTimeString: String): LocalDateTime =
    LocalDateTime.parse(dateTimeString, formatterDateTime)

  implicit val sendorDataFormat: Format[SensorData] = (
    (JsPath \ "Temperature").format[Float] and
      (JsPath \ "Humidity").format[Float]
  )(SensorData.apply, unlift(SensorData.unapply))

  implicit val SensorDataMessageFormat: Format[SensorDataMessage] = (
    (JsPath \ "Time").format[LocalDateTime] and
      (JsPath \ "AM2301").format[SensorData] and
      (JsPath \ "TempUnit").format[String]
  )(SensorDataMessage.apply, unlift(SensorDataMessage.unapply))
}
