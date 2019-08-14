val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.1.9"
val akkaStream = "com.typesafe.akka" %% "akka-stream" % "2.5.23"
val cakesolution = "net.cakesolutions" %% "scala-kafka-client-akka" % "2.1.0"
val kafkastreams = "org.apache.kafka" %% "kafka-streams-scala" % "2.2.1"
val playjson = "ai.x" %% "play-json-extensions" % "0.40.2"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.7" % "test"

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
    scalacOptions in (Compile, console) ~= (_.filterNot(Set("-Ywarn-unused:imports", "-Xfatal-warnings"))),
    libraryDependencies ++= List(akkaHttp, akkaStream, cakesolution, playjson, kafkastreams, scalaTest),
    resolvers ++=  Seq(Resolver.bintrayRepo("cakesolutions", "maven"), "confluent" at "http://packages.confluent.io/maven/", Resolver.jcenterRepo),
    scalafmtVersion := "1.5.1",
    scalafmtOnCompile in Compile := !sys.env.contains("DISABLE_SCALAFMT_ON_COMPILE"),
    scalafmtTestOnCompile in Compile := !sys.env.contains("DISABLE_SCALAFMT_ON_COMPILE"),
  )
