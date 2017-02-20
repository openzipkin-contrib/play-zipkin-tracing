package jp.co.bizreach.trace.play24.implicits

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
      span = tracer.newSpan(req.headers)((headers, key) => headers.get(key))
    )
  }

}
