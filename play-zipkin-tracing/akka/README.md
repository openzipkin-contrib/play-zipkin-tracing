play-zipkin-tracing-akka
========

A library to add tracing capability to Akka Actor.

## Setup

Add following dependency to `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "io.zipkin.brave.play" %% "play-zipkin-tracing-akka" % "3.0.0"
)
```

## Usage

### For standalone applications

This is an example of traceable actors. The parent actor is `HelloWorldActor` and it calls `HelloWorldChildActor`.

```scala
case class HelloWorldMessage(message: String)
  (implicit val traceData: ActorTraceData) extends TraceMessage

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

### For Play applications

Play offers [Akka integration](https://www.playframework.com/documentation/2.7.x/ScalaAkka). If you are using play-zipkin-tracing, you can track actor calls from a Play application as well. At first, let's take a look actors called from a Play application:

```scala
case class HelloActorMessage(message: String)
  (implicit val traceData: ActorTraceData) extends TraceMessage

class HelloActor @Inject()(@Named("child-hello-actor") child: ActorRef)
                          (implicit val tracer: ZipkinTraceServiceLike) extends TraceableActor {
  def receive = {
    case m: HelloActorMessage => {
      Thread.sleep(1000)
      println(m.message)
      TraceableActorRef(child) ! HelloActorMessage("This is a child actor call!")
      sender() ! "result"
    }
  }
}

class ChildHelloActor @Inject()(val tracer: ZipkinTraceServiceLike) extends TraceableActor {
  def receive = {
    case m: HelloActorMessage => {
      Thread.sleep(1000)
      println(m.message)
    }
  }
}
```

Next, below is a controller which calls `HelloActor` above:

```scala
class IndexController @Inject() (
  @Named("hello-actor") helloActor: ActorRef,
  components: ControllerComponents,
  service: ApiSampleService
) (
  implicit ec: ExecutionContext,
  val tracer: ZipkinTraceServiceLike
) extends AbstractController(components) with ZipkinTraceImplicits {

  def nest = Action.async { implicit req: Request[_] =>
    Logger.debug(req.headers.toSimpleMap.map{ case (k, v) => s"${k}:${v}"}.toSeq.mkString("\n"))

    implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)
    // Call an actor
    val f1 = TraceableActorRef(helloActor) ? HelloActorMessage("This is an actor call!")
    // Call a web service
    val f2 = service.sample("http://localhost:9992/api/nest")
    
    // Composite futures
    for {
      r1 <- f1
      r2 <- f2
    } yield Ok(Json.obj("result" -> (r1 + " " + r2)))
  }
}
```
