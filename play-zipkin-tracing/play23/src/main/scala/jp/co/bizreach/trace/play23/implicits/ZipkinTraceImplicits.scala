package jp.co.bizreach.trace.play23.implicits

import jp.co.bizreach.trace.{TraceCassette, TraceImplicits}
import jp.co.bizreach.trace.play23.ZipkinTraceService
import jp.co.bizreach.trace.zipkin.ZipkinTraceCassette
import play.api.libs.ws._
import play.api.mvc.RequestHeader

/**
  * Created by nishiyama on 2016/12/08.
  */
trait ZipkinTraceImplicits extends TraceImplicits {

  implicit def request2trace(implicit req: RequestHeader): TraceCassette = {
    ZipkinTraceCassette(
      span = ZipkinTraceService.toSpan(req.headers)((headers, key) => headers.get(key))
    )
  }

  implicit class RichWSRequest(r: WSRequestHolder) {
    def withTraceHeader()(implicit cassette: TraceCassette): WSRequestHolder = {
      r.withHeaders(ZipkinTraceService.toMap(cassette).toSeq: _*)
    }
  }

}
