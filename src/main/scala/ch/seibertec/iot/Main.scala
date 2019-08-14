package ch.seibertec.iot

import java.time.{LocalDateTime, LocalTime}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import ch.seibertec.iot.config.{IotKafkaConfig, IotKafkaConfigAcessor}
import ch.seibertec.iot.consumer.{
  AverageTemperatureStreamBuilder,
  IotEventListener
}
import com.typesafe.config.Config

object Main extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("lcm-service")
  implicit val materializer = ActorMaterializer()

  val configAccessor: IotKafkaConfigAcessor = IotKafkaConfig()
  val consumerConfig: Config = configAccessor.kafkaEventConsumer
  private val sensortopicjson = "sensor"
  IotEventListener(consumerConfig, sensortopicjson)
  new AverageTemperatureStreamBuilder(sensortopicjson, configAccessor).newStream
  val bindingFuture =
    Http().bindAndHandle(new WebRoute().route, "localhost", 8090)

  sys.ShutdownHookThread {
    println("Shutting ActorSysten")
    actorSystem.terminate()
  }
}


