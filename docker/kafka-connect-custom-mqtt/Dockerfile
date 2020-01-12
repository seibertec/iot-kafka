FROM confluentinc/cp-kafka-connect:5.1.0
COPY kafka-connect-mqtt-1.2.1-2.1.0-all-withSourceTopic.jar /etc/kafka-connect/lenses/
ENV CONNECT_PLUGIN_PATH "/usr/share/java,/etc/kafka-connect/jars,/etc/kafka-connect/lenses"