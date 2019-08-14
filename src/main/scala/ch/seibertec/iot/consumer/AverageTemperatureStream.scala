package ch.seibertec.iot.consumer

import ch.seibertec.iot.config.IotKafkaConfigAcessor
import ch.seibertec.iot.consumer.stream.{
  BaseStream,
  IotKafkaStreamConfiguration,
  IotSerdes
}
import ch.seibertec.iot.domain.SensorDataMessage
import ch.seibertec.iot.events.{Average, SensorData, TemperatureEvent}
import ch.seibertec.iot.events.internal.AggregatedSensorData
import org.apache.kafka.streams.scala.StreamsBuilder
import play.api.libs.json.Json

class AverageTemperatureStreamBuilder(topic: String,
                                      val config: IotKafkaConfigAcessor)
    extends IotKafkaStreamConfiguration {
  def newStream =
    new AverageTemperatureStream(topic, config)
}

class AverageTemperatureStream(topic: String, val config: IotKafkaConfigAcessor)
    extends IotKafkaStreamConfiguration
    with BaseStream
    with IotSerdes {

  override def applicationId: String = "IOTKAFKA_AverageTemperatureStream"

  implicit def convertToSensorData(
      sensorDataMessage: SensorDataMessage): SensorData = {
    SensorData(
      Some(sensorDataMessage.time.getDayOfMonth),
      Some(sensorDataMessage.time.getMonthValue),
      Some(sensorDataMessage.time.getYear),
      Some(sensorDataMessage.time.getHour),
      Some(sensorDataMessage.time.getMinute),
      Some(sensorDataMessage.data.temperature.toString)
    )
  }

  def newMinimumYearly(accumulatorForDate: AggregatedSensorData,
                       sensorData: SensorData): Option[SensorData] = ???

  def newMaximumYearly(accumulatorForDate: AggregatedSensorData,
                       sensorData: SensorData): Option[SensorData] = ???

  def newMinimumMonthly(accumulatorForDate: AggregatedSensorData,
                        sensorData: SensorData): Option[SensorData] = ???

  def newMaximumMonthly(accumulatorForDate: AggregatedSensorData,
                        sensorData: SensorData): Option[SensorData] = ???

  def newMinimumDaily(accumulatorForDate: AggregatedSensorData,
                      sensorData: SensorData): Option[SensorData] = ???

  def newMaximumDaily(accumulatorForDate: AggregatedSensorData,
                      sensorData: SensorData): Option[SensorData] = ???

  def newAverageTemperature(accumulatorForDate: AggregatedSensorData,
                            sensorData: SensorData): Option[String] = ???

  override def build(builder: StreamsBuilder): Unit = {

    import org.apache.kafka.streams.scala.ImplicitConversions._
    import org.apache.kafka.streams.scala.Serdes.String
    import ch.seibertec.iot.domain.SensorDataMessage._

    builder
      .stream[String, String](topic)
      .peek((k, v) => println(s"AverageTemperatureStream start: $k -> $v"))
      .map((k, v) => {
        val sensorDataMessage = Json.parse(v).as[SensorDataMessage]
        val sensorData = convertToSensorData(sensorDataMessage)
        (sensorDataMessage.sourceTopic, sensorData)
      })
      .filter((k, v) =>
        v.year.isDefined && v.month.isDefined && v.day.isDefined && v.hour.isDefined && v.minute.isDefined && v.temperature.isDefined)
      .groupByKey
      .aggregate(AggregatedSensorData(Seq.empty))(
        (k, v, acc) => {
          val accumulatorForDate =
            getAccumulatorForMonthAndYear(acc, v)
          accumulatorForDate.copy(
            sensorDataOfMonth = acc.sensorDataOfMonth :+ v,
            minimumYearly = newMinimumYearly(accumulatorForDate, v),
            maximumYearly = newMaximumYearly(accumulatorForDate, v),
            minimumMonthly = newMinimumMonthly(accumulatorForDate, v),
            maximumMonthly = newMaximumMonthly(accumulatorForDate, v),
            minimumDaily = newMinimumDaily(accumulatorForDate, v),
            maximumDaily = newMaximumDaily(accumulatorForDate, v),
            averageTemperatureOfMonth =
              newAverageTemperature(accumulatorForDate, v)
          )

        }
      )
      .toStream
      .mapValues(v =>
        TemperatureEvent(
          v.day,
          v.month,
          v.year,
          Some(Average(v.averageTemperatureOfMonth)),
          v.minimumDaily,
          v.maximumDaily,
          v.minimumMonthly,
          v.maximumMonthly,
          v.minimumYearly,
          v.maximumYearly
      ))
      .to("AverageTemTopic")

  }

  private def getAccumulatorForMonthAndYear(acc: AggregatedSensorData,
                                            sensorData: SensorData) = {
    val minMax = sensorData
    val year = sensorData.year.get
    val month = sensorData.month.get
    val dayOfMonth = sensorData.day.get
    val temperature = sensorData.temperature.get

    acc match {
      case AggregatedSensorData(_,
                                Some(m),
                                Some(y),
                                Some(d),
                                _,
                                _,
                                _,
                                _,
                                _,
                                _,
                                _)
          if y == year && m == month && d == dayOfMonth =>
        acc
      case AggregatedSensorData(_,
                                Some(m),
                                Some(y),
                                Some(d),
                                _,
                                _,
                                _,
                                _,
                                _,
                                _,
                                _)
          if y == year && m == month && d != dayOfMonth =>
        acc.copy(day = Some(dayOfMonth),
                 maximumDaily = Some(minMax),
                 minimumDaily = Some(minMax))
      case AggregatedSensorData(_,
                                Some(m),
                                Some(y),
                                Some(d),
                                _,
                                _,
                                _,
                                _,
                                _,
                                _,
                                _) if y == year && m < month =>
        acc.copy(
          month = Some(month),
          day = Some(dayOfMonth),
          maximumMonthly = Some(minMax),
          minimumMonthly = Some(minMax),
          maximumDaily = Some(minMax),
          minimumDaily = Some(minMax),
          averageTemperatureOfMonth = Some(temperature)
        )
      case AggregatedSensorData(_,
                                Some(m),
                                Some(y),
                                Some(d),
                                _,
                                _,
                                _,
                                _,
                                _,
                                _,
                                _) if y < year =>
        setBaseValues(acc, dayOfMonth, month, year, temperature, minMax)
      case _ =>
        setBaseValues(acc, dayOfMonth, month, year, temperature, minMax)

    }
  }

  private def setBaseValues(acc: AggregatedSensorData,
                            dayOfMonth: Int,
                            month: Int,
                            year: Int,
                            temperature: String,
                            minMax: SensorData) = {
    acc.copy(
      year = Some(year),
      month = Some(month),
      day = Some(dayOfMonth),
      maximumMonthly = Some(minMax),
      minimumMonthly = Some(minMax),
      maximumDaily = Some(minMax),
      minimumDaily = Some(minMax),
      averageTemperatureOfMonth = Some(temperature)
    )
  }

  start(streamingConfig)
}
