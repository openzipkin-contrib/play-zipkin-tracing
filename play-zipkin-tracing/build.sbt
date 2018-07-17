lazy val commonSettings = Seq(
  organization := "jp.co.bizreach",
  version := "2.1.1-SNAPSHOT",
  scalaVersion := "2.12.6",
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := (
    <url>https://github.com/bizreach/play-zipkin-tracing</url>
    <licenses>
      <license>
        <name>The Apache Software License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <scm>
      <url>https://github.com/bizreach/play-zipkin-tracing</url>
      <connection>scm:git:https://github.com/bizreach/play-zipkin-tracing.git</connection>
    </scm>
    <developers>
      <developer>
        <id>nishiyama</id>
        <name>Hajime Nishiyama</name>
      </developer>
      <developer>
        <id>shimamoto</id>
        <name>Takako Shimamoto</name>
      </developer>
      <developer>
        <id>takezoe</id>
        <name>Naoki Takezoe</name>
      </developer>
    </developers>
  )
)

val playVersion = "2.6.15"
val akkaVersion = "2.5.11"

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "play-zipkin-tracing",
    publishArtifact := false
  ).
  aggregate(core, akka, play)

lazy val core = (project in file("core")).
  settings(commonSettings: _*).
  settings(
    name := "play-zipkin-tracing-core",
    libraryDependencies ++= Seq(
      "commons-lang" % "commons-lang" % "2.6",
      "io.zipkin.brave" % "brave" % "4.12.0",
      "io.zipkin.reporter2" % "zipkin-sender-okhttp3" % "2.2.0",
      "org.scalatest" %% "scalatest" % "3.0.3" % "test",
      "io.zipkin.brave" % "brave-tests" % "4.12.0" % "test",
      "junit" % "junit" % "4.12" % "test",
      "com.novocode" % "junit-interface" % "0.11" % "test"
    ),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v")
  )

lazy val akka = (project in file("akka")).
  settings(commonSettings: _*).
  settings(
    name := "play-zipkin-tracing-akka",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion % Provided
    )
  ).dependsOn(
    core % "test->test;compile->compile"
  )

lazy val play = (project in file("play")).
  settings(commonSettings: _*).
  settings(
    name := "play-zipkin-tracing-play",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % playVersion % Provided,
      "com.typesafe.play" %% "play-ws" % playVersion % Provided,
      "com.typesafe.play" %% "play-guice" % playVersion % Test
    )
  ).dependsOn(
    core % "test->test;compile->compile",
    akka % "test->test;compile->compile"
  )
