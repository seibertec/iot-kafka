val akkaHttp = "com.typesafe.akka"         %% "akka-http"                      % "10.1.9"
val akkaStream = "com.typesafe.akka"        %% "akka-stream"                   % "2.5.23"
val cakesolution = "net.cakesolutions"         %% "scala-kafka-client-akka"        % "2.1.0"
val customMqttLensesConverter = "com.datamountaineer" % "kafka-connect-common" % "1.1.8"
val apacheKafkaConnect = "org.apache.kafka" % "connect-json" % "2.0.0"
val playjson=   "ai.x"                      %% "play-json-extensions"           % "0.40.2"
val kafkastreams =     "org.apache.kafka"          %% "kafka-streams-scala"     % "2.2.1"
val streamsavroserde = "io.confluent"              % "kafka-streams-avro-serde"        % "5.2.1"
val avroserialzer =  "io.confluent"              % "kafka-avro-serializer"           % "5.2.1"
val avro = "org.apache.avro"           % "avro"                            % "1.8.2"




lazy val root = (project in file("."))
  .settings(name := "iot-kafka")
  .settings(scalaVersion := "2.12.8")
  .settings(
    avroSettings,
    libraryDependencies ++= List(akkaHttp, akkaStream, cakesolution, playjson, kafkastreams, streamsavroserde, avroserialzer, avro, customMqttLensesConverter, apacheKafkaConnect)
  )


def unpackjar(jars: Seq[(File, String)], to: File): Unit =
  jars.map { jar =>
    val qualifiedTo = to / jar._2
    println(s"Processing $jar and saving to $qualifiedTo")
    IO.unzip(jar._1, qualifiedTo)
  }

val unpackAvro = Def.task {
  val jars = (update in Compile).value
    .select(configurationFilter("compile"))
    .flatMap { x =>
      x.name match {
        case _ => None
      }
    }

  val to = (sourceManaged in Compile).value / "avro"
  IO.delete(to)
  unpackjar(jars, to)
  IO.copyDirectory((sourceDirectory in Compile).value / "avro",to)
  Seq.empty[File]
}

val avroSettings = Seq(
  compile in Compile := (compile in Compile).dependsOn(unpackAvro).value,
  sourceGenerators in Compile += (avroScalaGenerateSpecific in Compile).taskValue,
  avroSpecificSourceDirectories in Compile := Seq((sourceManaged in Compile).value / "avro")
)
