package ch.seibertec.iot.consumer.util

import java.util

import io.confluent.kafka.serializers.{
  AbstractKafkaAvroDeserializer,
  KafkaAvroDeserializerConfig
}

class TypedKafkaAvroDeserializer[S](configValues: scala.Predef.Map[String, _])
    extends AbstractKafkaAvroDeserializer
    with org.apache.kafka.common.serialization.Deserializer[S] {

  import scala.collection.JavaConverters._
  private var isKey = false
  configure(configValues.asJava)

  def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {
    this.isKey = isKey
    configure((configValues ++ configs.asScala).asJava)

  }

  private def configure(configs: util.Map[String, _]): Unit = {
    this.configure(new KafkaAvroDeserializerConfig(configs))
  }

  def deserialize(topic: String, data: Array[Byte]): S = {
    super.deserialize(data).asInstanceOf[S]
  }

  def close(): Unit = {}
}
