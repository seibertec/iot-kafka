val akkaVersion = "2.5.23"

val akkaHttp = "com.typesafe.akka"         %% "akka-http"                      % "10.1.9"
val akkaStream = "com.typesafe.akka"        %% "akka-stream"                   % akkaVersion
val cakesolution = "net.cakesolutions"         %% "scala-kafka-client-akka"        % "2.1.0"
val playjson=   "ai.x"                      %% "play-json-extensions"           % "0.40.2"
val kafkastreams =     "org.apache.kafka"          %% "kafka-streams-scala"     % "2.2.1"
val streamsavroserde = "io.confluent"              % "kafka-streams-avro-serde"        % "5.2.1"
val avroserialzer =  "io.confluent"              % "kafka-avro-serializer"           % "5.2.1"
val avro = "org.apache.avro"           % "avro"                            % "1.8.2"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.7" % "test"
val slf4jAkka = "com.typesafe.akka"         %% "akka-slf4j"                     % akkaVersion
val logBack = "ch.qos.logback"            % "logback-classic"                 % "1.2.3"
val mockedstreams= "com.madewithtea"           %% "mockedstreams"                  % "3.3.0" % "test"


lazy val root = (project in file("."))
  .settings(
    name := "iot-kafka",
    scalaVersion := "2.12.8",
    scalacOptions := Seq(
      "-unchecked",
      "-Xexperimental",
      "-feature",
      "-language:implicitConversions",
      "-language:reflectiveCalls",
      "-deprecation",
      "-encoding",
      "utf8",
      //        "-Xfatal-warnings", // Fail the compilation if there are any warnings.
      "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
      "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
      "-Xlint:package-object-classes", // Class or object defined in package object.
      "-Xlint:unsound-match", // Pattern match may not be typesafe.
      // "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
      // ^^ ExpiryEventTableDefinition: Fails with polymorphic expression cannot be instantiated to expected type
      "-Xlint:adapted-args" // Warn if an argument list is modified to match the receiver.
      // "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
    ),
    avroSettings,
    scalacOptions in (Compile, console) ~= (_.filterNot(Set("-Ywarn-unused:imports", "-Xfatal-warnings"))),
    libraryDependencies ++= List(akkaHttp, akkaStream, cakesolution, playjson, kafkastreams, streamsavroserde, avroserialzer, avro, scalaTest, slf4jAkka, logBack, mockedstreams),
    resolvers ++=  Seq(Resolver.bintrayRepo("cakesolutions", "maven"), "confluent" at "http://packages.confluent.io/maven/", Resolver.jcenterRepo),
    scalafmtVersion := "1.5.1",
    scalafmtOnCompile in Compile := !sys.env.contains("DISABLE_SCALAFMT_ON_COMPILE"),
    scalafmtTestOnCompile in Compile := !sys.env.contains("DISABLE_SCALAFMT_ON_COMPILE"),
    fork in run := true,
    trapExit := false // in order to avoid "hanging around" after the application exits
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
