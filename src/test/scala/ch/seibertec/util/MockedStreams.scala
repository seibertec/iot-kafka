package ch.seibertec.util

import java.io.File
import java.nio.file.DirectoryNotEmptyException
import java.time.Instant
import java.util.{Properties, UUID}

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.utils.Utils
import org.apache.kafka.streams.errors.StreamsException
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.state.ReadOnlyWindowStore
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.apache.kafka.streams.{
  StreamsConfig,
  Topology,
  TopologyTestDriver => Driver
}
import kafka.utils.Implicits._

import scala.collection.JavaConverters._
import scala.collection.immutable

object MockedStreams {

  def apply() = Builder()

  case class Builder(topology: Option[() => Topology] = None,
                     configuration: Properties = new Properties(),
                     stateStores: Seq[String] = Seq(),
                     inputs: List[ConsumerRecord[Array[Byte], Array[Byte]]] =
                       List.empty,
                     initializer: Option[Driver => Unit] = None) {

    def config(configuration: Properties): Builder =
      this.copy(configuration = configuration)

    def topology(func: StreamsBuilder => Unit): Builder = {
      val buildTopology = () => {
        val builder = new StreamsBuilder()
        func(builder)
        builder.build()
      }
      this.copy(topology = Some(buildTopology))
    }

    def withTopology(t: () => Topology): Builder = this.copy(topology = Some(t))

    def stores(stores: Seq[String]): Builder = this.copy(stateStores = stores)

    def input[K, V](topic: String,
                    key: Serde[K],
                    value: Serde[V],
                    records: Seq[(K, V)]): Builder =
      _input(topic, key, value, Left(records))

    def inputS[K, V](topic: String, records: Seq[(K, V)])(
        implicit key: Serde[K],
        value: Serde[V]): Builder =
      _input(topic, key, value, Left(records))

    def inputWithTime[K, V](topic: String,
                            key: Serde[K],
                            value: Serde[V],
                            records: Seq[(K, V, Long)]): Builder =
      _input(topic, key, value, Right(records))

    def outputS[K, V](topic: String, size: Int)(
        implicit key: Serde[K],
        value: Serde[V]): immutable.IndexedSeq[(K, V)] =
      output(topic, key, value, size)

    def output[K, V](topic: String,
                     key: Serde[K],
                     value: Serde[V],
                     size: Int): immutable.IndexedSeq[(K, V)] = {
      if (size <= 0) throw new ExpectedOutputIsEmpty
      withProcessedDriver { driver =>
        (0 until size).flatMap { _ =>
          Option(driver.readOutput(topic, key.deserializer, value.deserializer)) match {
            case Some(record) => Some((record.key, record.value))
            case None         => None
          }
        }
      }
    }

    def outputTable[K, V](topic: String,
                          key: Serde[K],
                          value: Serde[V],
                          size: Int): Map[K, V] =
      output[K, V](topic, key, value, size).toMap

    def stateTable(name: String): Map[Nothing, Nothing] = withProcessedDriver {
      driver =>
        val records = driver.getKeyValueStore(name).all()
        val list = records.asScala.toList.map { record =>
          (record.key, record.value)
        }
        records.close()
        list.toMap
    }

    /**
      * @throws IllegalArgumentException if duration is negative or can't be represented as long milliseconds
      */
    def windowStateTable[K, V](
        name: String,
        key: K,
        timeFrom: Long = 0,
        timeTo: Long = Long.MaxValue): Map[java.lang.Long, V] =
      windowStateTable[K, V](name,
                             key,
                             Instant.ofEpochMilli(timeFrom),
                             Instant.ofEpochMilli(timeTo))

    /**
      * @throws IllegalArgumentException if duration is negative or can't be represented as long milliseconds
      */
    def windowStateTable[K, V](name: String,
                               key: K,
                               timeFrom: Instant,
                               timeTo: Instant): Map[java.lang.Long, V] =
      withProcessedDriver { driver =>
        val store =
          driver.getStateStore(name).asInstanceOf[ReadOnlyWindowStore[K, V]]
        val records = store.fetch(key, timeFrom, timeTo)
        val list = records.asScala.toList.map { record =>
          (record.key, record.value)
        }
        records.close()
        list.toMap
      }

    private def _input[K, V](
        topic: String,
        key: Serde[K],
        value: Serde[V],
        records: Either[Seq[(K, V)], Seq[(K, V, Long)]]) = {
      val keySer = key.serializer
      val valSer = value.serializer
      val factory = new ConsumerRecordFactory[K, V](keySer, valSer)

      val updatedRecords = records match {
        case Left(withoutTime) =>
          withoutTime.foldLeft(inputs) {
            case (events, (k, v)) => events :+ factory.create(topic, k, v)
          }
        case Right(withTime) =>
          withTime.foldLeft(inputs) {
            case (events, (k, v, timestamp)) =>
              events :+ factory.create(topic, k, v, timestamp)
          }
      }
      this.copy(inputs = updatedRecords)
    }

    // state store is temporarily created in ProcessorTopologyTestDriver
    private def stream: Driver = {
      val props = new Properties
      props.put(StreamsConfig.APPLICATION_ID_CONFIG,
                s"mocked-${UUID.randomUUID().toString}")
      props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
      props ++= configuration
      new Driver(topology.getOrElse(throw new NoTopologySpecified)(), props)
    }

    private def produce(driver: Driver): Unit = {
      initializer.foreach(_(driver))
      inputs.foreach(driver.pipeInput)

    }

    private def withProcessedDriver[T](f: Driver => T): T = {
      if (inputs.isEmpty) throw new NoInputSpecified

      val driver = stream
      produce(driver)
      val result: T = f(driver)
      try {
        driver.close()
      } catch {
        //Windows is having issues properly cleaning up the state
        //There is a .lock file that does not get removed until the finally clause of
        //org.apache.kafka.streams.processor.internals.StateDirectory.cleanRemovedTasks(long, boolean)
        //This case will trigger a DirectoryNotEmptyException and we have to catch it to continue working with streams
        case e: StreamsException
            if e.getCause.isInstanceOf[DirectoryNotEmptyException] =>
          Utils.delete(
            new File(
              e.getCause.asInstanceOf[DirectoryNotEmptyException].getFile))
      }
      result
    }
  }

  class NoTopologySpecified
      extends Exception("No topology specified. Call topology() on builder.")

  class NoInputSpecified
      extends Exception(
        "No input fixtures specified. Call input() method on builder.")

  class ExpectedOutputIsEmpty
      extends Exception("Output size needs to be greater than 0.")

}
