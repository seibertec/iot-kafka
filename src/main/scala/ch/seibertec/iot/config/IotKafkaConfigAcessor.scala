package ch.seibertec.iot.config

import akka.actor.ActorSystem
import com.typesafe.config.Config
import io.confluent.kafka.serializers.{
  AbstractKafkaAvroSerDeConfig,
  KafkaAvroDeserializerConfig
}

trait IotKafkaConfigAcessor {
  def config: Config

  private val iotConfig: Config = config.getConfig("iotKafka")
  def port: Int = iotConfig.getInt("port")
  private val kafkaConfig: Config = iotConfig.getConfig("kafka")
  def maxRetriesForProducts: Int = iotConfig.getInt("maxRetriesForProducts")
  def delayForProducts: Long = iotConfig.getLong("delayForProducts")

  def bootstrapServers: String = kafkaConfig.getString("bootstrapServers")
  def schemaRegistryUrl: String = kafkaConfig.getString("schemaRegistryUrl")
  def replicationFactor: Int = kafkaConfig.getInt("replicationFactor")
  def enableConfluentInterceptors: Boolean =
    kafkaConfig.getBoolean("enableConfluentInterceptors")
  def producerMessagesAcknowledge: String =
    kafkaConfig.getString("producerMessagesAcknowledge")
  def kafkaEventConsumer: Config = kafkaConfig.getConfig("kafkaEventConsumer")
  def globalStateStoreDirectory: String = kafkaConfig.getString("storeBasePath")

  def schemaRegistry: Map[String, String] =
    scala.Predef.Map(
      AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> schemaRegistryUrl,
      KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG -> true.toString
    )

}

class IotKafkaConfigAcessorImpl(system: ActorSystem)
    extends IotKafkaConfigAcessor {
  lazy val config: Config = system.settings.config
}

object IotKafkaConfig {

  def apply()(implicit system: ActorSystem): IotKafkaConfigAcessor =
    new IotKafkaConfigAcessorImpl(system)

}
