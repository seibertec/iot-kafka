package ch.seibertec.iot.config

import akka.actor.ActorSystem
import com.typesafe.config.Config

trait IotKafkaConfigAcessor {
   def config: Config

    private val iotConfig: Config  = config.getConfig("iotKafka")
    private val kafkaConfig: Config  = iotConfig.getConfig("kafka")

    def kafkaEventConsumer:  Config = kafkaConfig.getConfig("kafkaEventConsumer")

  }

class IotKafkaConfigAcessorImpl(system: ActorSystem) extends IotKafkaConfigAcessor {
  lazy val config: Config = system.settings.config
}

object IotKafkaConfig {

  def apply()(implicit system: ActorSystem): IotKafkaConfigAcessor =
    new IotKafkaConfigAcessorImpl(system)

}
