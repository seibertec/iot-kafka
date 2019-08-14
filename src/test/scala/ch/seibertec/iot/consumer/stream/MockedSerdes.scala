package ch.seibertec.iot.consumer.stream

import java.util.Collections

import ch.seibertec.iot.events.internal.AggregatedSensorData
import ch.seibertec.iot.events.{SensorData, TemperatureEvent}
import ch.seibertec.iot.stream.MockSchemaRegistryClient
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.avro.specific.SpecificRecord
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes.StringSerde

object MockedSerdes {

  class MockAvroSerde[T <: SpecificRecord](
      implicit client: SchemaRegistryClient)
      extends SpecificAvroSerde[T](client) {
    configure(Collections.singletonMap(
                AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                "http://fake.url"),
              false)
  }

  implicit val schemaRegistryConfig: SchemaRegistryConfig =
    SchemaRegistryConfig("UNUSED")
  implicit lazy val schemaRegistryClient: MockSchemaRegistryClient =
    new MockSchemaRegistryClient()

  schemaRegistryClient.register("TemperatureEvent-value",
                                TemperatureEvent.SCHEMA$)
  schemaRegistryClient.register("SensorData-value", SensorData.SCHEMA$)
  schemaRegistryClient.register("AggregatedSensorData-value",
                                AggregatedSensorData.SCHEMA$)

  implicit lazy val TemperatureEventSerde: Serde[TemperatureEvent] =
    new MockAvroSerde[TemperatureEvent]
  implicit lazy val SerdeAggregatedSensorData: Serde[AggregatedSensorData] =
    new MockAvroSerde[AggregatedSensorData]
  implicit lazy val SensorDataSerde: Serde[SensorData] =
    new MockAvroSerde[SensorData]

  implicit val stringSerde = new StringSerde

}
