organization := "jp.co.bizreach"

name := """zipkin-api-play26"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  jdbc,
  ehcache,
  ws,
  guice
)

val AkkaVersion = "2.5.8"

libraryDependencies ++= Seq(
  "jp.co.bizreach" %% "play-zipkin-tracing-play26" % "2.0.1"
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
