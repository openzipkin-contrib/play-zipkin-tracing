package jp.co.bizreach.trace.play25.implicits

import javax.inject.Inject

import jp.co.bizreach.trace.zipkin.{ZipkinTraceCassette, ZipkinTraceServiceLike}
import jp.co.bizreach.trace.{TraceCassette, TraceImplicits}
import play.api.libs.ws.WSRequest
import play.api.mvc.RequestHeader

/**
  * Created by nishiyama on 2016/12/08.
  */
trait ZipkinTraceImplicits extends TraceImplicits {

  val tracer: ZipkinTraceServiceLike

  implicit def request2trace(implicit req: RequestHeader): TraceCassette = {
    ZipkinTraceCassette(
      span = tracer.toSpan(req.headers)((headers, key) => headers.get(key))
    )
  }

  implicit class RichWSRequest(r: WSRequest) {
    def withTraceHeader()(implicit cassette: TraceCassette): WSRequest = {
      r.withHeaders(tracer.toMap(cassette).toSeq: _*)
    }
  }

}
