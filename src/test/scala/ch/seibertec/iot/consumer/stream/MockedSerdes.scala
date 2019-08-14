package ch.seibertec.iot.consumer.stream

import java.util.Collections

import ch.seibertec.iot.events.TemperatureEvent
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.avro.specific.SpecificRecord
import io.confluent.kafka.schemaregistry.client.{
  MockSchemaRegistryClient,
  SchemaRegistryClient
}
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

  schemaRegistryClient.register("TemperatureTopic-value",
                                TemperatureEvent.SCHEMA$)

  implicit lazy val TemperatureTopicSerde: Serde[TemperatureEvent] =
    new MockAvroSerde[TemperatureEvent]

  implicit val stringSerde = new StringSerde

}
