package ch.seibertec.iot.consumer.stream

import java.util
import java.util.{Collections, Properties}

import ch.seibertec.iot.config.IotKafkaConfigAcessor
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.streams.processor.WallclockTimestampExtractor
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.streams.state.RocksDBConfigSetter
import org.rocksdb.{BlockBasedTableConfig, Options}
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.avro.specific.SpecificRecord

case class SchemaRegistryConfig(url: String)
class AvroSerde[T <: SpecificRecord](keySerde: Boolean = false)(
    implicit cfg: SchemaRegistryConfig)
    extends SpecificAvroSerde[T] {
  configure(Collections.singletonMap(
              AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
              cfg.url),
            keySerde)
}

class CustomRocksDBConfig extends RocksDBConfigSetter {

  override def setConfig(storeName: String,
                         options: Options,
                         configs: util.Map[String, AnyRef]): Unit = {
    val tableConfig = new BlockBasedTableConfig
    tableConfig.setBlockCacheSize(50 * 1024 * 1024L)
    tableConfig.setBlockSize(4096L)
    tableConfig.setCacheIndexAndFilterBlocks(true)
    options.setTableFormatConfig(tableConfig)
    options.setMaxWriteBufferNumber(3)
  }
}

trait KafkaStreamConfiguration {

  def bootstrapServers: String
  def schemaRegistryUrl: String
  def replicationFactor: Int
  def producerMessagesAcknowledge: String
  def basePath: String
  def listenPort: String
  def enableConfluentInterceptors: Boolean

  lazy implicit val schemaRegistryConfig: SchemaRegistryConfig =
    SchemaRegistryConfig(schemaRegistryUrl)

  def applicationId: String

  val maxPoll: java.lang.Integer = 300
  val maxPollMs: java.lang.Integer = 900000
  val cacheSizeInBytes: java.lang.Long = 0L

  import org.apache.kafka.streams.StreamsConfig

  def streamingConfig: Properties = {
    val settings = new Properties
    settings.put(StreamsConfig.REPLICATION_FACTOR_CONFIG,
                 replicationFactor: java.lang.Integer)
    settings.put(StreamsConfig.producerPrefix(ProducerConfig.ACKS_CONFIG),
                 producerMessagesAcknowledge)
    settings.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG,
                 cacheSizeInBytes) // disable caching
    settings.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId)
    settings.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    settings.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPoll)
    settings.put(StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG,
                 classOf[CustomRocksDBConfig])
    settings.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollMs)
    settings.put(StreamsConfig.STATE_DIR_CONFIG,
                 s"$basePath/$listenPort/stores")
    settings.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG,
                 classOf[WallclockTimestampExtractor])

    if (enableConfluentInterceptors) {
      settings.put(
        StreamsConfig.PRODUCER_PREFIX + ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
        "io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor"
      )
      settings.put(
        StreamsConfig.CONSUMER_PREFIX + ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG,
        "io.confluent.monitoring.clients.interceptor.MonitoringConsumerInterceptor"
      )
    }
    settings
  }
}

trait IotKafkaStreamConfiguration extends KafkaStreamConfiguration {

  def config: IotKafkaConfigAcessor

  override val bootstrapServers: String = config.bootstrapServers
  override val schemaRegistryUrl: String = config.schemaRegistryUrl
  override val replicationFactor: Int = config.replicationFactor
  override val producerMessagesAcknowledge: String =
    config.producerMessagesAcknowledge
  override val basePath: String = config.globalStateStoreDirectory
  override val listenPort: String = config.port.toString
  override val enableConfluentInterceptors: Boolean =
    config.enableConfluentInterceptors
  override def applicationId: String = "IOTSENSOR"

  val maxRetriesForProducts: Int = config.maxRetriesForProducts
  val delayForProducts: Long = config.delayForProducts

}
