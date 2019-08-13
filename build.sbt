val akkaHttp = "com.typesafe.akka"         %% "akka-http"                      % "10.1.9"
val akkaStream = "com.typesafe.akka"        %% "akka-stream"                   % "2.5.23"
val cakesolution = "net.cakesolutions"         %% "scala-kafka-client-akka"        % "2.1.0"
val customMqttLensesConverter = "com.datamountaineer" % "kafka-connect-common" % "1.1.8"
val apacheKafkaConnect = "org.apache.kafka" % "connect-json" % "2.0.0"
val playjson=   "ai.x"                      %% "play-json-extensions"           % "0.40.2"
val kafkastreams =     "org.apache.kafka"          %% "kafka-streams-scala"     % "2.2.1"

lazy val root = (project in file("."))
  .settings(name := "iot-kafka")
  .settings(scalaVersion := "2.12.8")
  .settings(
    libraryDependencies ++= List(akkaHttp, akkaStream, cakesolution, playjson, kafkastreams, customMqttLensesConverter, apacheKafkaConnect)
  )