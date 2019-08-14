package ch.seibertec.iot.consumer.stream

import ch.seibertec.iot.events.TemperatureTopic
import ch.seibertec.iot.events.internal.AggregatedSensorData
import org.apache.kafka.common.serialization.Serde

trait IotSerdes {
  implicit val schemaRegistryConfig: SchemaRegistryConfig
  implicit lazy val SerdeTemperatureTopic: Serde[TemperatureTopic] =
    new AvroSerde[TemperatureTopic]
  implicit lazy val SerdeAggregatedSensorData: Serde[AggregatedSensorData] =
    new AvroSerde[AggregatedSensorData]

}
