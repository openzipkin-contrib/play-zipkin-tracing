package jp.co.bizreach.trace.play23.implicits

import java.math.BigInteger

import com.beachape.zipkin.HttpHeaders
import jp.co.bizreach.trace.service.TraceServiceLike
import jp.co.bizreach.trace.service.zipkin.ZipkinTraceCassette
import com.twitter.zipkin.gen.Span
import jp.co.bizreach.trace.service.{TraceCassette, TraceServiceLike}
import play.api.libs.ws.{WSRequest, WSRequestHolder}
import play.api.mvc.RequestHeader

import scala.util.Try

/**
  * Created by nishiyama on 2016/12/08.
  */
trait ZipkinRequestImplicits {
  implicit def req2trace(implicit rh: RequestHeader): TraceCassette = {
    ZipkinTraceCassette(req2span(rh))
  }

  implicit def req2span(implicit req: RequestHeader): Span = {
    val span = new Span
    import HttpHeaders._
    def hexStringToLong(s: String): Long = {
      new BigInteger(s, 16).longValue()
    }
    def ghettoBind(headerKey: HttpHeaders.Value): Option[Long] = for {
      idString <- req.headers.get(headerKey.toString)
      id <- Try(hexStringToLong(idString)).toOption
    } yield id
    ghettoBind(TraceIdHeaderKey).foreach(span.setTrace_id)
    ghettoBind(SpanIdHeaderKey).foreach(span.setId)
    ghettoBind(ParentIdHeaderKey).foreach(span.setParent_id)
    span
  }
}

trait WSImplicits {

  implicit class WSRequestOps(val r: WSRequestHolder) {
    def withTraceHeader()(implicit cassette: Option[TraceCassette], traceService: TraceServiceLike): WSRequestHolder = {
      cassette.fold(r)(c => r.withHeaders(traceService.toMap(c).toSeq: _*))
    }
  }
}