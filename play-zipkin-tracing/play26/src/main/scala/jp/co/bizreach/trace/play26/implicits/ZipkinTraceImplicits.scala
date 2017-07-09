package jp.co.bizreach.trace.play26.implicits

import brave.Span
import jp.co.bizreach.trace.play26.AkkaSupport.ActorTraceData
import jp.co.bizreach.trace.{TraceData, ZipkinTraceServiceLike}
import play.api.mvc.RequestHeader

trait ZipkinTraceImplicits {

  // for injection
  val tracer: ZipkinTraceServiceLike

  /**
   * Creates a trace data including a span from request headers.
   *
   * @param req the HTTP request header
   * @return the trace data
   */
  implicit def request2trace(implicit req: RequestHeader): TraceData = {
    TraceData(
      span = tracer.toSpan(req.headers)((headers, key) => headers.get(key))
    )
  }

  implicit def request2actorTrace(implicit req: RequestHeader): ActorTraceData = {
    val span = tracer.toSpan(req.headers)((headers, key) => headers.get(key))
    val oneWaySpan = tracer.tracing.tracer.newChild(span.context()).kind(Span.Kind.CLIENT)
    ActorTraceData(span = oneWaySpan)
  }

}
