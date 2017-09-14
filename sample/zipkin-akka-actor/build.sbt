organization := "jp.co.bizreach"

name := """zipkin-akka-actor"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.3"

// TODO temporary
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "jp.co.bizreach" %% "play-zipkin-tracing-akka" % "2.0.0-SNAPSHOT",
  "com.typesafe.akka" %% "akka-actor" % "2.5.3"
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
