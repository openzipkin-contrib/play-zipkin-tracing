package jp.co.bizreach.trace.play26

import akka.AroundReceiveOverrideHack
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import brave.Span
import jp.co.bizreach.trace.ZipkinTraceServiceLike

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object AkkaSupport {

  // TODO TraceData is available for only local actors. We have to invent another solution for remote actors.
  trait TraceMessage {
    val traceData: ActorTraceData
  }

  case class ActorTraceData(span: Span)

  trait ZipkinTraceActor extends AroundReceiveOverrideHack {

    val tracer: ZipkinTraceServiceLike

    implicit var traceData: ActorTraceData = null

    override protected def aroundReceiveMessage(receive: Receive, msg: Any): Unit = {
      msg match {
        case traceMessage: TraceMessage =>
          val serverSpan = tracer.tracing.tracer.newChild(traceMessage.traceData.span.context()).kind(Span.Kind.SERVER)
          tracer.serverReceived(self.path.name, serverSpan)

          val oneWaySpan = tracer.tracing.tracer.newChild(serverSpan.context()).kind(Span.Kind.CLIENT)
          this.traceData = ActorTraceData(oneWaySpan)

          Try(super.aroundReceiveMessage(receive, msg)) match {
            case Failure(t) =>
              serverSpan.tag("failed", s"Finished with exception: ${t.getMessage}")
              tracer.serverSend(serverSpan)
              throw t
            case Success(_) =>
              tracer.serverSend(serverSpan)
          }
        case _ =>
          super.aroundReceiveMessage(receive, msg)
      }
    }
  }

  class TraceActorRef(actorRef: ActorRef, tracer: ZipkinTraceServiceLike){
    def ! [T <: TraceMessage](message: T): Unit = {
      actorRef ! message
      message.traceData.span.name(actorRef.path.name).start().flush()
    }

    def ? [T <: TraceMessage](message: T)(implicit timeout: Timeout): Future[Any] = {
      val f = actorRef ? message
      message.traceData.span.name(actorRef.path.name).start()
      f.onComplete {
        case Failure(t) =>
          message.traceData.span.tag("failed", s"Finished with exception: ${t.getMessage}")
          message.traceData.span.finish()
        case _ =>
          message.traceData.span.finish()
      }(tracer.executionContext)
      f
    }
  }

  object TraceActorRef {
    def apply(actorRef: ActorRef)(implicit tracer: ZipkinTraceServiceLike): TraceActorRef = {
      new TraceActorRef(actorRef, tracer)
    }
  }

}
