import com.typesafe.sbt.SbtNativePackager._

organization := "jp.co.bizreach"
name := """zipkin-api-sample-23"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws
)

libraryDependencies ++= Seq(
  "jp.co.bizreach" %% "play-zipkin-tracing-play23" % "1.2.0-SNAPSHOT"
)


PlayKeys.playDefaultPort := 9993

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
