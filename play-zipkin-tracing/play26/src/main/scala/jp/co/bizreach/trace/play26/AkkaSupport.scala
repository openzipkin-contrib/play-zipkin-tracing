package jp.co.bizreach.trace.play26

import akka.AroundReceiveOverrideHack
import akka.actor.ActorRef
import brave.Span
import jp.co.bizreach.trace.{TraceData, ZipkinTraceServiceLike}
import play.api.mvc.RequestHeader

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

          try {
            super.aroundReceiveMessage(receive, msg)
          } finally {
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
      message.traceData.span.start().flush()
    }
  }

  object TraceActorRef {
    def apply(actorRef: ActorRef)(implicit tracer: ZipkinTraceServiceLike): TraceActorRef = {
      new TraceActorRef(actorRef, tracer)
    }
  }

}
