package jp.co.bizreach.trace.play23.implicits

import jp.co.bizreach.trace.play23.ZipkinTraceService
import jp.co.bizreach.trace.TraceData
import play.api.mvc.RequestHeader

trait ZipkinTraceImplicits {

  /**
   * Creates a trace data including a span from request headers.
   *
   * @param req the HTTP request header
   * @return the trace data
   */
  implicit def request2trace(implicit req: RequestHeader): TraceData = {
    TraceData(
      span = ZipkinTraceService.newSpan(req.headers)((headers, key) => headers.get(key))
    )
  }

}
