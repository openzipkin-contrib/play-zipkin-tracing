package jp.co.bizreach.trace.play23.implicits

import jp.co.bizreach.trace.play23.ZipkinTraceService
import jp.co.bizreach.trace.TraceData
import play.api.mvc.RequestHeader

/**
  * Created by nishiyama on 2016/12/08.
  */
trait ZipkinTraceImplicits {

  implicit def request2trace(implicit req: RequestHeader): TraceData = {
    TraceData(
      span = ZipkinTraceService.newSpan(req.headers)((headers, key) => headers.get(key))
    )
  }

}
