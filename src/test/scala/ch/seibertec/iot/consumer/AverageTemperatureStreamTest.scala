package ch.seibertec.iot.consumer

import java.util.Properties

import org.apache.kafka.common.serialization.Serde
import ch.seibertec.iot.config.{ConfigSupport, IotKafkaConfigAcessor}
import ch.seibertec.iot.events.internal.AggregatedSensorData
import ch.seibertec.iot.events.{SensorData, TemperatureEvent}
import ch.seibertec.util.MockedStreams
import org.scalatest.{Matchers, WordSpec}

import scala.collection.immutable

class AverageTemperatureStreamTest
    extends WordSpec
    with Matchers
    with ConfigSupport {

  import org.apache.kafka.streams.scala.Serdes._
  import ch.seibertec.iot.consumer.stream.MockedSerdes._
  "AverageTemperatureStreamTest" must {
    "calculate daily min and max" in {

      val mstreams = MockedStreams()
        .topology { builder =>
          new MockedAverageTemperatureStreamBuilder()
            .stream(config)
            .build(builder)
        }
        .inputS(
          "sensor",
          Seq(
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-08-14T07:04:04\",\"AM2301\":{\"Temperature\":22.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-08-14T07:05:54\",\"AM2301\":{\"Temperature\":19.9,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-08-14T08:06:05\",\"AM2301\":{\"Temperature\":24.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-08-14T09:07:06\",\"AM2301\":{\"Temperature\":26.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-08-15T08:47:16\",\"AM2301\":{\"Temperature\":20.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-08-15T09:49:16\",\"AM2301\":{\"Temperature\":18.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}""")
          )
        )

      val result: immutable.Seq[(String, TemperatureEvent)] =
        mstreams
          .outputS[String, TemperatureEvent]("TemperatureStatistics", 100)

      result.size shouldBe 6

      result.head._2.minimumDaily.flatMap(_.temperature) shouldBe Some("22.0")
      result.head._2.minimumDaily.flatMap(_.minute) shouldBe Some(4)
      result.head._2.maximumDaily.flatMap(_.temperature) shouldBe Some("22.0")
      result.head._2.maximumDaily.flatMap(_.minute) shouldBe Some(4)

      result(1)._2.minimumDaily.flatMap(_.temperature) shouldBe Some("19.9")
      result(1)._2.minimumDaily.flatMap(_.minute) shouldBe Some(5)
      result(1)._2.maximumDaily.flatMap(_.temperature) shouldBe Some("22.0")
      result(1)._2.maximumDaily.flatMap(_.minute) shouldBe Some(4)

      result(2)._2.minimumDaily.flatMap(_.temperature) shouldBe Some("19.9")
      result(2)._2.minimumDaily.flatMap(_.minute) shouldBe Some(5)
      result(2)._2.maximumDaily.flatMap(_.temperature) shouldBe Some("24.0")
      result(2)._2.maximumDaily.flatMap(_.minute) shouldBe Some(6)

      result(3)._2.minimumDaily.flatMap(_.temperature) shouldBe Some("19.9")
      result(3)._2.minimumDaily.flatMap(_.minute) shouldBe Some(5)
      result(3)._2.maximumDaily.flatMap(_.temperature) shouldBe Some("26.0")
      result(3)._2.maximumDaily.flatMap(_.minute) shouldBe Some(7)

      result(4)._2.minimumDaily.flatMap(_.temperature) shouldBe Some("20.0")
      result(4)._2.minimumDaily.flatMap(_.day) shouldBe Some(15)
      result(4)._2.minimumDaily.flatMap(_.minute) shouldBe Some(47)
      result(4)._2.maximumDaily.flatMap(_.temperature) shouldBe Some("20.0")
      result(4)._2.maximumDaily.flatMap(_.minute) shouldBe Some(47)
      result(4)._2.maximumDaily.flatMap(_.day) shouldBe Some(15)

      result(5)._2.minimumDaily.flatMap(_.temperature) shouldBe Some("18.0")
      result(5)._2.minimumDaily.flatMap(_.day) shouldBe Some(15)
      result(5)._2.minimumDaily.flatMap(_.minute) shouldBe Some(49)
      result(5)._2.maximumDaily.flatMap(_.temperature) shouldBe Some("20.0")
      result(5)._2.maximumDaily.flatMap(_.minute) shouldBe Some(47)
      result(5)._2.maximumDaily.flatMap(_.day) shouldBe Some(15)

    }
    "calculate monthly min and max" in {

      val mstreams = MockedStreams()
        .topology { builder =>
          new MockedAverageTemperatureStreamBuilder()
            .stream(config)
            .build(builder)
        }
        .inputS(
          "sensor",
          Seq(
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-09-14T07:04:04\",\"AM2301\":{\"Temperature\":22.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-09-15T07:05:54\",\"AM2301\":{\"Temperature\":19.9,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-09-16T08:06:05\",\"AM2301\":{\"Temperature\":24.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-09-17T09:07:06\",\"AM2301\":{\"Temperature\":26.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-10-01T08:47:16\",\"AM2301\":{\"Temperature\":20.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-10-02T09:49:16\",\"AM2301\":{\"Temperature\":18.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}""")
          )
        )

      val result: immutable.Seq[(String, TemperatureEvent)] =
        mstreams
          .outputS[String, TemperatureEvent]("TemperatureStatistics", 100)

      result.size shouldBe 6

      result.head._2.minimumMonthly.flatMap(_.temperature) shouldBe Some("22.0")
      result.head._2.minimumMonthly.flatMap(_.day) shouldBe Some(14)
      result.head._2.minimumMonthly.flatMap(_.month) shouldBe Some(9)
      result.head._2.maximumMonthly.flatMap(_.temperature) shouldBe Some("22.0")
      result.head._2.maximumMonthly.flatMap(_.minute) shouldBe Some(4)

      result(1)._2.minimumMonthly.flatMap(_.temperature) shouldBe Some("19.9")
      result(1)._2.minimumMonthly.flatMap(_.day) shouldBe Some(15)
      result(1)._2.maximumMonthly.flatMap(_.temperature) shouldBe Some("22.0")
      result(1)._2.maximumMonthly.flatMap(_.minute) shouldBe Some(4)

      result(2)._2.minimumMonthly.flatMap(_.temperature) shouldBe Some("19.9")
      result(2)._2.minimumMonthly.flatMap(_.day) shouldBe Some(15)
      result(2)._2.maximumMonthly.flatMap(_.temperature) shouldBe Some("24.0")
      result(2)._2.maximumMonthly.flatMap(_.minute) shouldBe Some(6)

      result(3)._2.minimumMonthly.flatMap(_.temperature) shouldBe Some("19.9")
      result(3)._2.minimumMonthly.flatMap(_.day) shouldBe Some(15)
      result(3)._2.maximumMonthly.flatMap(_.temperature) shouldBe Some("26.0")
      result(3)._2.maximumMonthly.flatMap(_.minute) shouldBe Some(7)

      result(4)._2.minimumMonthly.flatMap(_.temperature) shouldBe Some("20.0")
      result(4)._2.minimumMonthly.flatMap(_.day) shouldBe Some(1)
      result(4)._2.minimumMonthly.flatMap(_.month) shouldBe Some(10)
      result(4)._2.maximumMonthly.flatMap(_.temperature) shouldBe Some("20.0")
      result(4)._2.maximumMonthly.flatMap(_.day) shouldBe Some(1)
      result(4)._2.maximumMonthly.flatMap(_.month) shouldBe Some(10)
      result(4)._2.maximumMonthly.flatMap(_.minute) shouldBe Some(47)

      result(5)._2.minimumMonthly.flatMap(_.temperature) shouldBe Some("18.0")
      result(5)._2.minimumMonthly.flatMap(_.day) shouldBe Some(2)
      result(5)._2.minimumMonthly.flatMap(_.month) shouldBe Some(10)
      result(5)._2.maximumMonthly.flatMap(_.temperature) shouldBe Some("20.0")
      result(5)._2.maximumMonthly.flatMap(_.day) shouldBe Some(1)
      result(5)._2.maximumMonthly.flatMap(_.month) shouldBe Some(10)
      result(5)._2.maximumMonthly.flatMap(_.minute) shouldBe Some(47)

    }

    "calculate yearly min and max" in {

      val mstreams = MockedStreams()
        .topology { builder =>
          new MockedAverageTemperatureStreamBuilder()
            .stream(config)
            .build(builder)
        }
        .inputS(
          "sensor",
          Seq(
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-09-14T07:04:04\",\"AM2301\":{\"Temperature\":22.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-10-15T07:05:54\",\"AM2301\":{\"Temperature\":19.9,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-11-16T08:06:05\",\"AM2301\":{\"Temperature\":24.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-12-17T09:07:06\",\"AM2301\":{\"Temperature\":26.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2020-01-01T08:47:16\",\"AM2301\":{\"Temperature\":20.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2020-02-02T09:49:16\",\"AM2301\":{\"Temperature\":18.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}""")
          )
        )

      val result: immutable.Seq[(String, TemperatureEvent)] =
        mstreams
          .outputS[String, TemperatureEvent]("TemperatureStatistics", 100)

      result.size shouldBe 6

      result.head._2.minimumYearly.flatMap(_.temperature) shouldBe Some("22.0")
      result.head._2.minimumYearly.flatMap(_.month) shouldBe Some(9)
      result.head._2.minimumYearly.flatMap(_.year) shouldBe Some(2019)
      result.head._2.maximumYearly.flatMap(_.temperature) shouldBe Some("22.0")
      result.head._2.maximumYearly.flatMap(_.month) shouldBe Some(9)
      result.head._2.maximumYearly.flatMap(_.year) shouldBe Some(2019)

      result(1)._2.minimumYearly.flatMap(_.temperature) shouldBe Some("19.9")
      result(1)._2.minimumYearly.flatMap(_.month) shouldBe Some(10)
      result(1)._2.minimumYearly.flatMap(_.year) shouldBe Some(2019)
      result(1)._2.maximumYearly.flatMap(_.temperature) shouldBe Some("22.0")
      result(1)._2.maximumYearly.flatMap(_.month) shouldBe Some(9)
      result(1)._2.maximumYearly.flatMap(_.year) shouldBe Some(2019)

      result(2)._2.minimumYearly.flatMap(_.temperature) shouldBe Some("19.9")
      result(2)._2.minimumYearly.flatMap(_.month) shouldBe Some(10)
      result(2)._2.minimumYearly.flatMap(_.year) shouldBe Some(2019)
      result(2)._2.maximumYearly.flatMap(_.temperature) shouldBe Some("24.0")
      result(2)._2.maximumYearly.flatMap(_.month) shouldBe Some(11)
      result(2)._2.maximumYearly.flatMap(_.year) shouldBe Some(2019)

      result(3)._2.minimumYearly.flatMap(_.temperature) shouldBe Some("19.9")
      result(3)._2.minimumYearly.flatMap(_.month) shouldBe Some(10)
      result(3)._2.minimumYearly.flatMap(_.year) shouldBe Some(2019)
      result(3)._2.maximumYearly.flatMap(_.temperature) shouldBe Some("26.0")
      result(3)._2.maximumYearly.flatMap(_.month) shouldBe Some(12)
      result(3)._2.maximumYearly.flatMap(_.year) shouldBe Some(2019)

      result(4)._2.minimumYearly.flatMap(_.temperature) shouldBe Some("20.0")
      result(4)._2.minimumYearly.flatMap(_.month) shouldBe Some(1)
      result(4)._2.minimumYearly.flatMap(_.year) shouldBe Some(2020)
      result(4)._2.maximumYearly.flatMap(_.temperature) shouldBe Some("20.0")
      result(4)._2.maximumYearly.flatMap(_.month) shouldBe Some(1)
      result(4)._2.maximumYearly.flatMap(_.year) shouldBe Some(2020)

      result(5)._2.minimumYearly.flatMap(_.temperature) shouldBe Some("18.0")
      result(5)._2.minimumYearly.flatMap(_.month) shouldBe Some(2)
      result(5)._2.minimumYearly.flatMap(_.year) shouldBe Some(2020)
      result(5)._2.maximumYearly.flatMap(_.temperature) shouldBe Some("20.0")
      result(5)._2.maximumYearly.flatMap(_.month) shouldBe Some(1)
      result(5)._2.maximumYearly.flatMap(_.year) shouldBe Some(2020)

    }

    "calculate monthly average" in {

      val mstreams = MockedStreams()
        .topology { builder =>
          new MockedAverageTemperatureStreamBuilder()
            .stream(config)
            .build(builder)
        }
        .inputS(
          "sensor",
          Seq(
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-09-14T07:04:04\",\"AM2301\":{\"Temperature\":22.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-09-15T07:05:54\",\"AM2301\":{\"Temperature\":19.9,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-09-16T08:06:05\",\"AM2301\":{\"Temperature\":24.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-09-17T09:07:06\",\"AM2301\":{\"Temperature\":26.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-10-01T08:47:16\",\"AM2301\":{\"Temperature\":20.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-10-02T09:49:16\",\"AM2301\":{\"Temperature\":18.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}""")
          )
        )

      val result: immutable.Seq[(String, TemperatureEvent)] =
        mstreams
          .outputS[String, TemperatureEvent]("TemperatureStatistics", 100)

      result.size shouldBe 6

      result.head._2.averageMonthly.flatMap(_.temperature) shouldBe Some("22.0")
      result(1)._2.averageMonthly.flatMap(_.temperature) shouldBe Some(
        ((22.0f + 19.9f) / 2).toString)
      result(2)._2.averageMonthly.flatMap(_.temperature) shouldBe Some(
        ((22.0f + 19.9f + 24.0f) / 3).toString)
      result(3)._2.averageMonthly.flatMap(_.temperature) shouldBe Some(
        ((22.0f + 19.9f + 24.0f + 26f) / 4).toString)
      result(4)._2.averageMonthly.flatMap(_.temperature) shouldBe Some("20.0")
      result(5)._2.averageMonthly.flatMap(_.temperature) shouldBe Some(
        ((20.0f + 18f) / 2).toString)

    }

  }
}

class MockedAverageTemperatureStreamBuilder() {

  def stream(config: IotKafkaConfigAcessor)(
      implicit
      sensorDataSerde: Serde[SensorData],
      temperatureEventSerde: Serde[TemperatureEvent],
      aggregatedSensorDataSerde: Serde[AggregatedSensorData]
  ) =
    new AverageTemperatureStream("sensor", config) {
      override def start(streamingConfig: Properties): Unit = {}
    }
}
