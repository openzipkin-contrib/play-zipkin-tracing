package jp.co.bizreach.trace.play25.implicits

import jp.co.bizreach.trace.{TraceData, ZipkinTraceServiceLike}
import play.api.mvc.RequestHeader

/**
  * Created by nishiyama on 2016/12/08.
  */
trait ZipkinTraceImplicits {

  val tracer: ZipkinTraceServiceLike

  implicit def request2trace(implicit req: RequestHeader): TraceData = {
    TraceData(
      span = tracer.newSpan(req.headers)((headers, key) => headers.get(key))
    )
  }

}
