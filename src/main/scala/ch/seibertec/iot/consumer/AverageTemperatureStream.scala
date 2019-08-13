package ch.seibertec.iot.consumer

import ch.seibertec.iot.config.IotKafkaConfigAcessor
import ch.seibertec.iot.consumer.stream.{BaseStream, IotKafkaStreamConfiguration}
import org.apache.kafka.streams.scala.StreamsBuilder


class AverageTemperatureStreamBuilder(topic: String, val config: IotKafkaConfigAcessor) extends IotKafkaStreamConfiguration {
  def newStream =
    new AverageTemperatureStream(topic, config)
}

class AverageTemperatureStream(topic: String, val config: IotKafkaConfigAcessor)     extends IotKafkaStreamConfiguration with BaseStream {

  override def applicationId: String = "IOTKAFKA_AverageTemperatureStream"


  override protected def build(builder: StreamsBuilder): Unit = {

    import org.apache.kafka.streams.scala.ImplicitConversions._
    import org.apache.kafka.streams.scala.Serdes.String

    builder
      .stream[String, String](topic)
      .peek((k, v) =>
        println(s"AverageTemperatureStream start: $k -> $v"))
      .to("AverageTemTopic")

  }
  start(streamingConfig)
}
