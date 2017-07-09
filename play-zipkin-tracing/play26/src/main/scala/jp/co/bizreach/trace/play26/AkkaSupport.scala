package jp.co.bizreach.trace.play26

import akka.AroundReceiveOverrideHack
import brave.Span
import jp.co.bizreach.trace.{TraceData, ZipkinTraceServiceLike}

object AkkaSupport {

  // TODO TraceData is available for only local actors. We have to invent another solution for remote actors.
  trait TraceMessage {
    val traceData: TraceData
  }

  trait ZipkinTraceActor extends AroundReceiveOverrideHack {

    val tracer: ZipkinTraceServiceLike

    implicit var traceData: TraceData = null

    override protected def aroundReceiveMessage(receive: Receive, msg: Any): Unit = {
      msg match {
        case traceMessage: TraceMessage =>
          val serverSpan = tracer.tracing.tracer.newChild(traceMessage.traceData.span.context()).kind(Span.Kind.SERVER)
          tracer.serverReceived(self.path.name, serverSpan)
          this.traceData = TraceData(serverSpan)
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

}
