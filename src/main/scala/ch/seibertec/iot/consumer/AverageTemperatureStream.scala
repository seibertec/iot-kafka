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
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.scala.StreamsBuilder
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json

class AverageTemperatureStreamBuilder(topic: String,
                                      val config: IotKafkaConfigAcessor)
    extends IotKafkaStreamConfiguration
    with IotSerdes {
  def newStream =
    new AverageTemperatureStream(topic, config)
}

class AverageTemperatureStream(topic: String, val config: IotKafkaConfigAcessor)(
    implicit
    sensorData: Serde[SensorData],
    temperatureEvent: Serde[TemperatureEvent],
    aggregatedSensorData: Serde[AggregatedSensorData]
) extends IotKafkaStreamConfiguration
    with BaseStream
    with IotSerdes {

  override def applicationId: String = "IOTKAFKA_AverageTemperatureStream"
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  implicit def convertToSensorData(
      sensorDataMessage: SensorDataMessage): SensorData = {
    val dateTimeString =
      SensorDataMessage.toLocalDateTimeString(sensorDataMessage.time)
    SensorData(
      Some(dateTimeString),
      Some(sensorDataMessage.time.getDayOfMonth),
      Some(sensorDataMessage.time.getMonthValue),
      Some(sensorDataMessage.time.getYear),
      Some(sensorDataMessage.time.getHour),
      Some(sensorDataMessage.time.getMinute),
      Some(sensorDataMessage.data.temperature.toString)
    )
  }

  def newMinimumYearly(accumulatorForDate: AggregatedSensorData,
                       sensorData: SensorData): Option[SensorData] =
    if (accumulatorForDate.minimumYearly.exists(m =>
          m.temperature.get.toFloat <= sensorData.temperature.get.toFloat))
      accumulatorForDate.minimumYearly
    else Some(sensorData)

  def newMaximumYearly(accumulatorForDate: AggregatedSensorData,
                       sensorData: SensorData): Option[SensorData] =
    if (accumulatorForDate.maximumYearly.exists(m =>
          m.temperature.get.toFloat > sensorData.temperature.get.toFloat))
      accumulatorForDate.maximumYearly
    else Some(sensorData)

  def newMinimumMonthly(accumulatorForDate: AggregatedSensorData,
                        sensorData: SensorData): Option[SensorData] =
    if (accumulatorForDate.minimumMonthly.exists(m =>
          m.temperature.get.toFloat <= sensorData.temperature.get.toFloat))
      accumulatorForDate.minimumMonthly
    else Some(sensorData)

  def newMaximumMonthly(accumulatorForDate: AggregatedSensorData,
                        sensorData: SensorData): Option[SensorData] =
    if (accumulatorForDate.minimumMonthly.exists(m =>
          m.temperature.get.toFloat >= sensorData.temperature.get.toFloat))
      accumulatorForDate.minimumMonthly
    else Some(sensorData)

  def newMinimumDaily(accumulatorForDate: AggregatedSensorData,
                      sensorData: SensorData): Option[SensorData] =
    if (accumulatorForDate.minimumDaily.exists(m =>
          m.temperature.get.toFloat <= sensorData.temperature.get.toFloat))
      accumulatorForDate.minimumDaily
    else Some(sensorData)

  def newMaximumDaily(accumulatorForDate: AggregatedSensorData,
                      sensorData: SensorData): Option[SensorData] =
    if (accumulatorForDate.maximumDaily.exists(m =>
          m.temperature.get.toFloat >= sensorData.temperature.get.toFloat))
      accumulatorForDate.maximumDaily
    else Some(sensorData)

  def newAverageTemperature(accumulatorForDate: AggregatedSensorData,
                            sensorData: SensorData): Option[String] = {

    Some(
      accumulatorForDate.sensorDataOfMonth
        .map(s => s.temperature.get.toFloat)
        .sum / accumulatorForDate.sensorDataOfMonth.size).map(_.toString)
  }
  override def build(builder: StreamsBuilder): Unit = {

    import org.apache.kafka.streams.scala.ImplicitConversions._
    import org.apache.kafka.streams.scala.Serdes.String
    import ch.seibertec.iot.domain.SensorDataMessage._

    builder
      .stream[String, String](topic)
      .peek((k, v) =>
        logger.info(
          s"AverageTemperatureStream receive input message: $k -> $v"))
      .map((k, v) => {
        val sensorDataMessage = Json.parse(v).as[SensorDataMessage]
        val sensorData = convertToSensorData(sensorDataMessage)
        (sensorDataMessage.sourceTopic, sensorData)
      })
      .filter((k, v) =>
        v.year.isDefined && v.month.isDefined && v.day.isDefined && v.hour.isDefined && v.minute.isDefined && v.temperature.isDefined)
      .peek((k, v) =>
        logger.info(s"AverageTemperatureStream after convert: $k -> $v"))
      .groupByKey
      .aggregate(AggregatedSensorData(sensorDataOfMonth = Seq.empty))(
        (k, v, acc) => {
          val accumulatorForDate = getAccumulatorForMonthAndYear(acc, v)
          val accumulatorForDateWithTimeSeries = accumulatorForDate.copy(
            sensorDataOfMonth = accumulatorForDate.sensorDataOfMonth :+ v)

          accumulatorForDateWithTimeSeries.copy(
            minimumYearly =
              newMinimumYearly(accumulatorForDateWithTimeSeries, v),
            maximumYearly =
              newMaximumYearly(accumulatorForDateWithTimeSeries, v),
            minimumMonthly =
              newMinimumMonthly(accumulatorForDateWithTimeSeries, v),
            maximumMonthly =
              newMaximumMonthly(accumulatorForDateWithTimeSeries, v),
            minimumDaily = newMinimumDaily(accumulatorForDateWithTimeSeries, v),
            maximumDaily = newMaximumDaily(accumulatorForDateWithTimeSeries, v),
            averageTemperatureOfMonth =
              newAverageTemperature(accumulatorForDateWithTimeSeries, v)
          )
        }
      )
      .toStream
      .mapValues(v =>
        TemperatureEvent(
          v.lastSensorDate,
          Some(Average(v.averageTemperatureOfMonth)),
          v.minimumDaily,
          v.maximumDaily,
          v.minimumMonthly,
          v.maximumMonthly,
          v.minimumYearly,
          v.maximumYearly
      ))
      .peek((k, v) => logger.info(s"AverageTemperatureStream output: $k -> $v"))
      .to("TemperatureStatistics")

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
                                _,
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
        acc.copy(lastSensorDate = sensorData.sensorDate)
      case AggregatedSensorData(_,
                                _,
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
        acc.copy(lastSensorDate = sensorData.sensorDate,
                 day = Some(dayOfMonth),
                 maximumDaily = Some(minMax),
                 minimumDaily = Some(minMax))
      case AggregatedSensorData(_,
                                _,
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
          lastSensorDate = sensorData.sensorDate,
          month = Some(month),
          day = Some(dayOfMonth),
          maximumMonthly = Some(minMax),
          minimumMonthly = Some(minMax),
          maximumDaily = Some(minMax),
          minimumDaily = Some(minMax),
          averageTemperatureOfMonth = Some(temperature),
          sensorDataOfMonth = Nil
        )
      case AggregatedSensorData(_,
                                _,
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
        setBaseValues(acc,
                      dayOfMonth,
                      month,
                      year,
                      temperature,
                      minMax,
                      sensorData.sensorDate)
      case _ =>
        setBaseValues(acc,
                      dayOfMonth,
                      month,
                      year,
                      temperature,
                      minMax,
                      sensorData.sensorDate)

    }
  }

  private def setBaseValues(acc: AggregatedSensorData,
                            dayOfMonth: Int,
                            month: Int,
                            year: Int,
                            temperature: String,
                            minMax: SensorData,
                            sensorDate: Option[String]) = {
    acc.copy(
      lastSensorDate = sensorDate,
      year = Some(year),
      month = Some(month),
      day = Some(dayOfMonth),
      minimumYearly = Some(minMax),
      maximumYearly = Some(minMax),
      maximumMonthly = Some(minMax),
      minimumMonthly = Some(minMax),
      maximumDaily = Some(minMax),
      minimumDaily = Some(minMax),
      averageTemperatureOfMonth = Some(temperature),
      sensorDataOfMonth = Nil
    )
  }

  start(streamingConfig)
}
