package ch.seibertec.iot.consumer

import java.util.UUID

import akka.actor.{
  Actor,
  ActorLogging,
  ActorRef,
  ActorSystem,
  Props,
  Terminated
}
import cakesolutions.kafka.KafkaConsumer
import cakesolutions.kafka.akka.KafkaConsumerActor.{Confirm, Subscribe}
import cakesolutions.kafka.akka.{ConsumerRecords, KafkaConsumerActor}
import ch.seibertec.iot.config.IotKafkaConfig
import ch.seibertec.iot.consumer.util.TypedKafkaAvroDeserializer
import ch.seibertec.iot.events.TemperatureEvent
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.duration._

object IotStatisticsListener {

  /*
   * Starts an ActorSystem and instantiates the below Actor that subscribes and
   * consumes from the configured KafkaConsumerActor.
   *
   */
  def apply(config: Config, topic: String)(
      implicit as: ActorSystem): ActorRef = {

    val consumerConf: KafkaConsumer.Conf[String, TemperatureEvent] =
      KafkaConsumer
        .Conf(
          new StringDeserializer,
          new TypedKafkaAvroDeserializer[TemperatureEvent](
            IotKafkaConfig.apply().schemaRegistry),
          groupId = s"test_group${UUID.randomUUID()}",
          enableAutoCommit = false,
          autoOffsetReset = OffsetResetStrategy.EARLIEST
        )
        .withConf(config)

    val actorConf = KafkaConsumerActor.Conf(1.seconds, 3.seconds)

    val system = ActorSystem()
    system.actorOf(
      Props(new IotStatisticsListener(topic, consumerConf, actorConf)),
      s"kafka-consumer-actor${UUID.randomUUID()}")
  }
}

class IotStatisticsListener(
    topic: String,
    kafkaConfig: KafkaConsumer.Conf[String, TemperatureEvent],
    actorConfig: KafkaConsumerActor.Conf)
    extends Actor
    with ActorLogging {

  private var statisticsData: Map[String, List[TemperatureEvent]] = Map.empty.withDefaultValue(List.empty[TemperatureEvent])
  private val recordsExt = ConsumerRecords.extractor[String, TemperatureEvent]

  private val consumer = context.actorOf(
    KafkaConsumerActor.props(kafkaConfig, actorConfig, self)
  )
  context.watch(consumer)

  consumer ! Subscribe.AutoPartition(List(topic))

  override def receive: Receive = {

    // Records from Kafka
    case recordsExt(records) =>
      processRecords(records.pairs)
      sender() ! Confirm(records.offsets, commit = true)

    case Terminated(s) =>
      println(s"KafkaConsumer terminated ...")

  }

  private def processRecords(
      records: Seq[(Option[String], TemperatureEvent)]) = {
    import ch.seibertec.iot.domain.SensorDataMessage._
    records.foreach {
      case (key, value) =>
        println(s"Received [$value]")
        val keyString = key.getOrElse("UNKNOWN")
        statisticsData = statisticsData + (keyString-> (value :: statisticsData(keyString)))
    }
  }
}
