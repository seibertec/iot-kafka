package ch.seibertec.iot.consumer.stream

import ch.seibertec.iot.events.{SensorData, TemperatureEvent}
import ch.seibertec.iot.events.internal.AggregatedSensorData
import org.apache.kafka.common.serialization.Serde

trait IotSerdes {
  implicit val schemaRegistryConfig: SchemaRegistryConfig
  implicit lazy val SerdeTemperatureTopic: Serde[TemperatureEvent] =
    new AvroSerde[TemperatureEvent]
  implicit lazy val SerdeSensorData: Serde[SensorData] =
    new AvroSerde[SensorData]
  implicit lazy val SerdeAggregatedSensorData: Serde[AggregatedSensorData] =
    new AvroSerde[AggregatedSensorData]

}
