package ch.seibertec.iot.config

import com.typesafe.config.{Config, ConfigFactory}

trait ConfigSupport {

  val config: IotKafkaConfigAcessor = new IotKafkaConfigAcessor {
    override def config: Config = ConfigFactory.load()
  }

}
