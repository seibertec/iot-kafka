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
    "map calculate daily min" in {

      val mstreams = MockedStreams()
        .topology { builder =>
          new MockedAverageTemperatureStreamBuilder()
            .stream(config)
            .build(builder)
        }
        .inputS(
          "sensortopicjson",
          Seq(
            ("key",
             s"""{\"SourceTopic\":\"tele/sonoffWaedi/SENSOR\",\"Time\":\"2019-08-14T07:47:04\",\"AM2301\":{\"Temperature\":24.0,\"Humidity\":52.4},\"TempUnit\":\"C\"}"""),
          )
        )

      val result: immutable.Seq[(String, TemperatureEvent)] =
        mstreams
          .outputS[String, TemperatureEvent]("TemperatureStatistics", 100)

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
    new AverageTemperatureStream("sensortopicjson", config) {
      override def start(streamingConfig: Properties): Unit = {}
    }
}
