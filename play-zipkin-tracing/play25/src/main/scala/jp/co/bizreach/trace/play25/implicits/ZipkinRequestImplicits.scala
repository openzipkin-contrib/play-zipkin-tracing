package jp.co.bizreach.trace.play25.implicits

import java.math.BigInteger

import jp.co.bizreach.trace.service.zipkin.ZipkinTraceCassette
import com.twitter.zipkin.gen.Span
import jp.co.bizreach.trace.service.{TraceCassette, TraceServiceLike}
import jp.co.bizreach.trace.service.zipkin.SpanHttpHeaders
import play.api.libs.ws._
import play.api.mvc.RequestHeader

import scala.util.Try

/**
  * Created by nishiyama on 2016/12/08.
  */
trait ZipkinRequestImplicits {
  implicit def req2trace(implicit rh: RequestHeader): ZipkinTraceCassette = {
    ZipkinTraceCassette(req2span(rh), req2sampled(rh))
  }

  implicit def req2sampled(implicit req: RequestHeader): Boolean = {
    import jp.co.bizreach.trace.service.zipkin.TraceHttpHeaders._
    req.headers.get(SampledHeaderKey.toString).forall {
      case "0" => false
      case "1" => true
      case _ => true
    }
  }

  implicit def req2span(implicit req: RequestHeader): Span = {
    val span = new Span
    import SpanHttpHeaders._
    def hexStringToLong(s: String): Long = {
      new BigInteger(s, 16).longValue()
    }
    def ghettoBind(headerKey: SpanHttpHeaders.Value): Option[Long] = for {
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

  implicit class WSRequestOps(val r: WSRequest) {
    def withTraceHeader()(implicit cassette: Option[TraceCassette], traceService: TraceServiceLike): WSRequest = {
      cassette.fold(r)(c => r.withHeaders(traceService.toMap(c).toSeq: _*))
    }
  }
}