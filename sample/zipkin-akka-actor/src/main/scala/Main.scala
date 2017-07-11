import java.util.concurrent.TimeUnit

import actors.{HelloWorldActor, HelloWorldMessage}
import akka.actor._
import akka.util.Timeout
import brave.Span
import jp.co.bizreach.trace.akka.actor.ActorTraceSupport._
import jp.co.bizreach.trace.akka.actor.ZipkinTraceService

import scala.concurrent._
import scala.concurrent.duration.Duration

object Main extends App {

  val system = ActorSystem("mySystem")
  implicit val tracer = new ZipkinTraceService(system, "zipkin-akka-actor")
  implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)

  val actor = system.actorOf(Props(classOf[HelloWorldActor], tracer), "parent-actor")

  // TODO Won't create an initial span explicitly...
  val span = tracer.tracing.tracer.newTrace().kind(Span.Kind.CLIENT)
  val initialTraceData = ActorTraceData(span)

  val f = TraceableActorRef(actor) ? HelloWorldMessage("Test")(initialTraceData)
  val result = Await.result(f, Duration.Inf)
  println(result)

  Thread.sleep(1000)
  tracer.close()
  system.terminate()

}
