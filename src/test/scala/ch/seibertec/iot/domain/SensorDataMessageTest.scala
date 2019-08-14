package ch.seibertec.iot.domain

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

class SensorDataMessageTest extends WordSpec with Matchers {

  private val formatterDateTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME
  def toLocalDateTime(dateTimeString: String): LocalDateTime =
    LocalDateTime.parse(dateTimeString, formatterDateTime)

  "SensorDataMessage" must {
    "Convert from Json Message" in {

      val message =
        s"""{"SourceTopic":"tele/sonoffWaedi/SENSOR","Time":"2019-08-14T07:47:04","AM2301":{"Temperature":24.0,"Humidity":52.4},"TempUnit":"C"}"""
      val parsedMessage = Json.parse(message).as[SensorDataMessage]
      parsedMessage.sourceTopic shouldBe "tele/sonoffWaedi/SENSOR"
      parsedMessage.time shouldBe toLocalDateTime("2019-08-14T07:47:04")
      parsedMessage.data.temperature shouldBe 24.0f
      parsedMessage.data.humidity shouldBe 52.4f
      parsedMessage.unit shouldBe "C"
    }
  }

}
