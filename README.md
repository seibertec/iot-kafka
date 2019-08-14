# iot-kafka

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

## Links
Kafka control center: http://localhost:9021
 