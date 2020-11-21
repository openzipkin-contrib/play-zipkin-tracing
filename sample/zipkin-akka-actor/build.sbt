organization := "io.zipkin.brave.play"

name := """zipkin-akka-actor"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.11"

libraryDependencies ++= Seq(
  "io.zipkin.brave.play" %% "play-zipkin-tracing-akka" % "3.0.2",
  "com.typesafe.akka"    %% "akka-actor" % "2.5.32"
)

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
