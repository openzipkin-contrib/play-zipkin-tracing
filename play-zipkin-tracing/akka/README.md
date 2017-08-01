play-zipkin-tracing-akka
========

A library to add tracing capability to Akka Actor.

## Setup

Add following dependency to `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "jp.co.bizreach" %% "play-zipkin-tracing-akka" % "1.4.0-SNAPSHOT"
)
```

## Usage

### In standalone program

This is an example of traceable actors. The parent actor is `HelloWorldActor` and it calls `HelloWorldChildActor`.

```scala
class HelloWorldActor(implicit val tracer: ZipkinTraceServiceLike) extends TraceableActor {
  private val childActor = context.actorOf(Props(classOf[HelloWorldChildActor], tracer), "child-actor")
  implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)

  override def receive: Receive = {
    case m: HelloWorldMessage => {
      Thread.sleep(500)
      TraceableActorRef(childActor) ! HelloWorldMessage("Hello Child!")
      sender() ! "Response from parent actor."
    }
  }
}

class HelloWorldChildActor(implicit val tracer: ZipkinTraceServiceLike) extends TraceableActor {
  override def receive: Receive = {
    case m: HelloWorldMessage =>
      Thread.sleep(100)
  }
}

case class HelloWorldMessage(message: String)
  (implicit val traceData: ActorTraceData) extends TraceMessage
```

You can call `HelloWorldActor` as follows:

```scala
val system = ActorSystem("mySystem")
implicit val tracer = new ZipkinTraceService(system, "zipkin-akka-actor")
implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)

val actor = system.actorOf(Props(classOf[HelloWorldActor], tracer), "parent-actor")

val f = TraceableActorRef(actor) ? HelloWorldMessage("Test")(ActorTraceData())

val result = Await.result(f, Duration.Inf)
println(result)
```

The first point is messages must extend `TraceMessage` and have `traceData` field. The second point is wrapping an actor by `TraceableActorRef(...)` before calling.

For remote actors, a message class must extend `RemoteTraceMessage` instead of `TraceMessage`.

```scala
case class HelloWorldMessage(message: String)
  (implicit val traceData: RemoteActorTraceData) extends RemoteTraceMessage
```

### With Play application

TODO
