val akkaHttp = "com.typesafe.akka"         %% "akka-http"                      % "10.1.9"
val akkaStream = "com.typesafe.akka"        %% "akka-stream"                   % "2.5.23"
val cakesolution = "net.cakesolutions"         %% "scala-kafka-client-akka"        % "2.1.0"

lazy val root = (project in file("."))
  .settings(name := "iot-kafka")
  .settings(scalaVersion := "2.12.8")
  .settings(
    libraryDependencies ++= List(akkaHttp, akkaStream, cakesolution)
  )