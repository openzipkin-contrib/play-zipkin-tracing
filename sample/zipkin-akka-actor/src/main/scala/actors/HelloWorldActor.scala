package actors

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.util.Timeout
import jp.co.bizreach.trace.ZipkinTraceServiceLike
import jp.co.bizreach.trace.akka.actor.ActorTraceSupport._

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

case class HelloWorldMessage(message: String)(implicit val traceData: ActorTraceData) extends TraceMessage
