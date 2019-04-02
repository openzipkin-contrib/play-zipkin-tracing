organization := "io.zipkin.brave.play"

name := """zipkin-api-play27"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  ws,
  guice
)

libraryDependencies ++= Seq(
  "io.zipkin.brave.play" %% "play-zipkin-tracing-play" % "3.0.0"
)

PlayKeys.playDefaultPort := 9991

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-Xlint",
  "-Ywarn-dead-code",
  //  "-Ywarn-unused-import",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions"
)

javacOptions in compile ++= Seq(
  "-encoding", "UTF-8",
  "-source", "1.8",
  "-target", "1.8"
)
