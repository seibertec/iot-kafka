# iot-kafka

## Start required docker containers
Required for operation of this application are:
- MQTT broker (ponte)
- Kafka
- Zookeeper (required by Kafka)
- Schema registry (for supporting AVRO schemas on Kafka messages)
- Kafka rest proxy
- Kafka connect

Optional components - that may help development - are:
- KSQL (access Kafka topics with a SQL-like query language)
- KSQL-cli (command line interface - if no local ksql client is available)
- Confluent control center (monitor Kafka eco system)

All containers are described in `docker/docker-compose.yaml` and can be started like:
```shell script
➜ cd docker
➜ docker-compose -f docker-compose.yaml up
```

Verification of proper startup of the containers is possible with
```
➜ docker ps -a
CONTAINER ID        IMAGE                                             COMMAND                  CREATED             STATUS              PORTS                                                      NAMES
3a323094fb20        confluentinc/cp-enterprise-control-center:5.3.0   "/etc/confluent/dock…"   13 seconds ago      Up 11 seconds       0.0.0.0:9021->9021/tcp                                     control-center
07d4f6bc2588        confluentinc/cp-ksql-server:5.3.0                 "/etc/confluent/dock…"   14 seconds ago      Up 12 seconds       0.0.0.0:8088->8088/tcp                                     ksql-server
90e3f5397d52        confluentinc/cp-kafka-rest:5.3.0                  "/etc/confluent/dock…"   14 seconds ago      Up 12 seconds       0.0.0.0:8082->8082/tcp                                     rest-proxy
779eb2e3e19e        confluentinc/cp-schema-registry:5.3.0             "/etc/confluent/dock…"   15 seconds ago      Up 14 seconds       0.0.0.0:8081->8081/tcp                                     schema-registry
c9abf289726e        confluentinc/cp-kafka-connect:5.1.0               "/etc/confluent/dock…"   15 seconds ago      Up 14 seconds       0.0.0.0:8083->8083/tcp, 9092/tcp                           kafka-connect
4d1b935a0bba        confluentinc/cp-kafka:5.1.0                       "/etc/confluent/dock…"   16 seconds ago      Up 15 seconds       0.0.0.0:9092->9092/tcp                                     kafka
688ad5aefcc4        zookeeper:3.4.9                                   "/docker-entrypoint.…"   17 seconds ago      Up 16 seconds       2888/tcp, 0.0.0.0:2181->2181/tcp, 3888/tcp                 zookeeper
1685cf9eaa9a        mapboss/ponte                                     "/bin/sh -c 'ponte -…"   17 seconds ago      Up 16 seconds       0.0.0.0:1883->1883/tcp, 0.0.0.0:3000->3000/tcp, 5683/tcp   ponte
```

## Cleanup
If a complete cleanup is required and all containers and their state shall be deleted, execute the following steps:

### Remove docker containers and their docker volumes
```
➜ docker rm -f $(docker ps -a -q)
➜ docker volume rm $(d volume ls -q)
```
### Cleanup local persistence for Kafka, Zookeeper and the application
```
➜ cd docker
➜ rm -rf kafka zookeeper data
```

## Configure connector plugin
Configuration of the confluent mqtt connector plugin was not very straightforward and we changed to the lenses.io mqtt connector plugin (see https://docs.lenses.io/connectors/source/mqtt.html).

The connector plugin is packaged as jar and can be found in `docker/mqtt-lenses`. This directory is mounted to the kafka-connect container's `CONNECT_PLUGIN_PATH`.

We extended the existing lenses mqtt connector with a new message converter `SourceTopicPropagationSimpleJsonConverter` in order to propagate the original MQTT source topic into the JSON payload.
Otherwise it would not be possible to determine the original sender of the message (when using wildcards in the configuration).

A message produced by this converter looks like:
```json
{
  "SourceTopic":"tele/sonoffWaedi/SENSOR",
  "Time":"2019-08-14T08:18:51",
  "AM2301":{
    "Temperature":24.1,
    "Humidity":51.2,
  },
  "TempUnit":"C",
}
```

The connector is configured using this configuration file `docker/mqtt-lenses.conf` and the kafka-connect cli (see https://github.com/landoop/kafka-connect-tools/releases). 
Usage is:
```shell script
➜ kc-cli run mqtt-source-lenses < mqtt-lenses.conf
Validating connector properties before posting
Connector properties valid. Creating connector mqtt-source-lenses
#Connector `mqtt-source-lenses`:
name=mqtt-source-lenses
connect.mqtt.clean=true
connect.mqtt.timeout=1000
key.converter=org.apache.kafka.connect.json.JsonConverter
connect.mqtt.converter.throw.on.error=true
value.converter=org.apache.kafka.connect.json.JsonConverter
tasks.max=1
connect.progress.enabled=true
connect.mqtt.hosts=tcp://ponte:1883
connect.mqtt.client.id=dm_source_id,
key.converter.schemas.enable=false
connector.class=com.datamountaineer.streamreactor.connect.mqtt.source.MqttSourceConnector
connect.mqtt.service.quality=0
connect.mqtt.kcql=INSERT INTO kjson SELECT * FROM tele/+/SENSOR WITHCONVERTER=`com.datamountaineer.streamreactor.connect.mqtt.converter.SourceTopicPropagationSimpleJsonConverter`
connect.mqtt.keep.alive=1000
value.converter.schemas.enable=false
connect.mqtt.log.message=true
#task ids: 0
```
And this can be used to query all active connector plugins:
```shell script
➜   kc-cli ps
mqtt-source-avro
mqtt-source
mqtt-source-lenses
```

The connector plugins could also be configured using the REST api directly - if no cli is available. In this case the config file must be formatted as valid JSON:

`curl -d @connect-mqtt-source.json -H "Content-Type: application/json" -X POST http://localhost:8083/connectors`

## Access to the user interface
The web-based UI of the application can be accessed at http://localhost:8090/static/vue.html

## Links
Kafka control center: http://localhost:9021

 