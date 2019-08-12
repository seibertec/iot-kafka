package ch.seibertec.iot.consumer

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import cakesolutions.kafka.KafkaConsumer
import cakesolutions.kafka.akka.KafkaConsumerActor.{Confirm, Subscribe}
import cakesolutions.kafka.akka.{ConsumerRecords, KafkaConsumerActor}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.duration._


object IotEventListener {

  /*
   * Starts an ActorSystem and instantiates the below Actor that subscribes and
   * consumes from the configured KafkaConsumerActor.
   *
   */
  def apply(config: Config, topic: String = "sensortopic"): ActorRef = {

    val consumerConf = KafkaConsumer.Conf(
      new StringDeserializer,
      new StringDeserializer,
      groupId = s"test_group${UUID.randomUUID()}",
      enableAutoCommit = false,
      autoOffsetReset = OffsetResetStrategy.EARLIEST)
      .withConf(config)

    val actorConf = KafkaConsumerActor.Conf(1.seconds, 3.seconds)

    val system = ActorSystem()
    system.actorOf(Props(new IotEventListener(topic, consumerConf, actorConf)), s"kafka-consumer-actor${UUID.randomUUID()}")
  }
}

class IotEventListener(topic: String,
                             kafkaConfig: KafkaConsumer.Conf[String, String],
                             actorConfig: KafkaConsumerActor.Conf) extends Actor with ActorLogging {

  private var sensorData: List[String] = Nil
  private val recordsExt = ConsumerRecords.extractor[String, String]

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

  private def processRecords(records: Seq[(Option[String], String)]) =
    records.foreach { case (key, value) =>
      sensorData= value::sensorData
      println(s"Received [$value]")
    }
}

