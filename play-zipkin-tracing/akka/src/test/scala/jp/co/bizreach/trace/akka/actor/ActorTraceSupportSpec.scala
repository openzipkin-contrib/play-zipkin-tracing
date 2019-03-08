package jp.co.bizreach.trace.akka.actor

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import org.scalatest.AsyncFlatSpec
import ActorTraceSupport._
import jp.co.bizreach.trace.{TestZipkinTraceService, ZipkinTraceServiceLike}


class ActorTraceSupportSpec extends AsyncFlatSpec {

  it should "ask pattern" in {
    val system = ActorSystem("mySystem")
    implicit val tracer = new TestZipkinTraceService
    implicit val timeout = Timeout(5, TimeUnit.SECONDS)

    val actor = system.actorOf(Props(classOf[HelloWorldActor], tracer), "test-actor")

    TraceableActorRef(actor) ? HelloWorldMessage("Test", ActorTraceData()) map { result =>
      assert(result == "Received data: Test")
    }

    TimeUnit.SECONDS.sleep(3)
    tracer.tracing.close()
    system.terminate()

    assert(tracer.reporter.spans.length == 2)
    val parent = tracer.reporter.spans.find(_.name == "? - test-actor").get
    val child  = tracer.reporter.spans.find(_.name == "test-actor").get
    assert(parent.id == child.parentId)
    assert(parent.id != child.id)
  }

}

class HelloWorldActor(val tracer: ZipkinTraceServiceLike) extends TraceableActor {
  def receive = {
    case m: HelloWorldMessage =>
      sender() ! s"Received data: ${m.message}"
  }
}

case class HelloWorldMessage(message: String, traceData: ActorTraceData) extends TraceMessage