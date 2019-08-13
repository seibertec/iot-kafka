package ch.seibertec.iot.consumer

import ch.seibertec.iot.config.IotKafkaConfigAcessor
import ch.seibertec.iot.consumer.stream.{BaseStream, IotKafkaStreamConfiguration, IotSerdes}
import ch.seibertec.iot.domain.SensorDataMessage
import ch.seibertec.iot.events.{Average, MinMaxTemperature, TemperatureTopic}
import ch.seibertec.iot.events.internal.{AggregatedSensorData, SensorData}
import org.apache.kafka.streams.scala.StreamsBuilder
import play.api.libs.json.Json


class AverageTemperatureStreamBuilder(topic: String, val config: IotKafkaConfigAcessor) extends IotKafkaStreamConfiguration {
  def newStream =
    new AverageTemperatureStream(topic, config)
}

class AverageTemperatureStream(topic: String, val config: IotKafkaConfigAcessor)     extends IotKafkaStreamConfiguration with BaseStream with IotSerdes{

  override def applicationId: String = "IOTKAFKA_AverageTemperatureStream"

  implicit def convertToSensorData(v: String): SensorData = {
    val sensorDataMessage = Json.parse(v).as[SensorDataMessage]
    val dayOfMonth = sensorDataMessage.time.getDayOfMonth
    val month = sensorDataMessage.time.getMonthValue
    val year = sensorDataMessage.time.getYear
    SensorData(Some(dayOfMonth), Some(dayOfMonth), Some(year), Some(sensorDataMessage.data.temperature.toString))
  }

  def newMinimumYearly(accumulatorForDate: AggregatedSensorData, sensorData: SensorData): Option[MinMaxTemperature] = ???

  def newMaximumYearly(accumulatorForDate: AggregatedSensorData, sensorData: SensorData): Option[MinMaxTemperature] = ???

  def newMinimumMonthly(accumulatorForDate: AggregatedSensorData, sensorData: SensorData): Option[MinMaxTemperature] = ???

  def newMaximumMonthly(accumulatorForDate: AggregatedSensorData, sensorData: SensorData): Option[MinMaxTemperature] = ???

  def newMinimumDaily(accumulatorForDate: AggregatedSensorData, sensorData: SensorData): Option[MinMaxTemperature] = ???

  def newMaximumDaily(accumulatorForDate: AggregatedSensorData, sensorData: SensorData): Option[MinMaxTemperature] = ???

  def newAverageTemperature(accumulatorForDate: AggregatedSensorData, sensorData: SensorData): Option[String] = ???

  override protected def build(builder: StreamsBuilder): Unit = {

    import org.apache.kafka.streams.scala.ImplicitConversions._
    import org.apache.kafka.streams.scala.Serdes.String
    import ch.seibertec.iot.domain.SensorDataMessage._

    builder
      .stream[String, String](topic)
      .peek((k, v) =>
        println(s"AverageTemperatureStream start: $k -> $v"))
      .map((k,v)=> ("Sensorname", v))
      .groupByKey
      .aggregate(AggregatedSensorData(Seq.empty))(
        (k, v, acc) => {
          val sensorData: SensorData = convertToSensorData(v)
          val accumulatorForDate= getAccumulatorForMonthAndYear(acc, sensorData.day.get, sensorData.month.get, sensorData.year.get, sensorData.temperature.get)
          accumulatorForDate.copy(
            sensorDataOfMonth= acc.sensorDataOfMonth:+sensorData,
            minimumYearly= newMinimumYearly(accumulatorForDate, sensorData),
            maximumYearly= newMaximumYearly(accumulatorForDate, sensorData),
            minimumMonthly= newMinimumMonthly(accumulatorForDate, sensorData),
            maximumMonthly= newMaximumMonthly(accumulatorForDate, sensorData),
            minimumDaily= newMinimumDaily(accumulatorForDate, sensorData),
            maximumDaily= newMaximumDaily(accumulatorForDate, sensorData),
            averageTemperatureOfMonth = newAverageTemperature(accumulatorForDate, sensorData)
          )

        }
      )
      .toStream
      .mapValues(v=> TemperatureTopic(v.day,v.month, v.year,Some(Average(v.averageTemperatureOfMonth)), v.minimumDaily, v.maximumDaily, v.minimumMonthly, v.maximumMonthly,v.minimumYearly, v.maximumYearly))
      .to("AverageTemTopic")

  }


  private def getAccumulatorForMonthAndYear(acc: AggregatedSensorData, dayOfMonth: Int, month: Int, year: Int, temperature:String) = {
    val minMax= MinMaxTemperature(Some(dayOfMonth), Some(month), Some(year), Some(temperature))

    acc match {
      case AggregatedSensorData(_,Some(m), Some(y), Some(d),_,_,_,_,_, _,_) if y== year  &&  m == month && d == dayOfMonth =>
        acc
      case AggregatedSensorData(_,Some(m), Some(y), Some(d),_,_,_,_,_, _,_) if  y== year && m == month && d != dayOfMonth =>
        acc.copy( day = Some(dayOfMonth), maximumDaily= Some(minMax), minimumDaily= Some(minMax))
      case AggregatedSensorData(_,Some(m), Some(y), Some(d),_,_,_,_,_, _,_) if y== year && m < month  =>
        acc.copy(month = Some(month), day = Some(dayOfMonth),maximumMonthly= Some(minMax), minimumMonthly=Some(minMax), maximumDaily= Some(minMax), minimumDaily= Some(minMax), averageTemperatureOfMonth = Some(temperature))
      case AggregatedSensorData(_,Some(m), Some(y), Some(d),_,_,_,_,_, _,_) if y< year =>
        setBaseValues(acc, dayOfMonth, month, year, temperature, minMax)
      case _ =>
        setBaseValues(acc, dayOfMonth, month, year, temperature, minMax)

    }
  }

  private def setBaseValues(acc: AggregatedSensorData, dayOfMonth: Int, month: Int, year: Int, temperature: String, minMax: MinMaxTemperature) = {
    acc.copy(year = Some(year), month = Some(month), day = Some(dayOfMonth), maximumMonthly = Some(minMax), minimumMonthly = Some(minMax), maximumDaily = Some(minMax), minimumDaily = Some(minMax), averageTemperatureOfMonth = Some(temperature))
  }

  start(streamingConfig)
}
