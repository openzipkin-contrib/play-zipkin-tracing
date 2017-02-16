lazy val commonSettings = Seq(
  organization := "jp.co.bizreach",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.11.8"
)

val play25Version = "2.5.7"
val play23Version = "2.3.10"

lazy val root = (project in file(".")).
  settings(publish := {}).
  aggregate(core, play25, play23)

lazy val core = (project in file("core")).
  settings(commonSettings: _*).
  settings(
    name := "play-zipkin-tracing-core",
    libraryDependencies ++= Seq(
      "commons-lang" % "commons-lang" % "2.6",
      "io.zipkin.brave" % "brave" % "4.0.6",
      "io.zipkin.reporter" % "zipkin-sender-okhttp3" % "0.6.12"
    )
  )

lazy val play25 = (project in file("play25")).
  settings(commonSettings: _*).
  settings(
    name := "play-zipkin-tracing-play25",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % play25Version % Provided,
      "com.typesafe.play" %% "play-ws" % play25Version % Provided
    )
  ).dependsOn(core % "test->test;compile->compile")

lazy val play23 = (project in file("play23")).
  settings(commonSettings: _*).
  settings(
    name := "play-zipkin-tracing-play23",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % play23Version % Provided,
      "com.typesafe.play" %% "play-ws" % play23Version % Provided
    )
  ).dependsOn(core % "test->test;compile->compile")