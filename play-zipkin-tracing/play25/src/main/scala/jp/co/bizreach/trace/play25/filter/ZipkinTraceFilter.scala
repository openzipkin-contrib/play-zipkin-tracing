package jp.co.bizreach.trace.play25.filter

import javax.inject.Inject

import akka.stream.Materializer
import jp.co.bizreach.trace.zipkin.ZipkinTraceServiceLike
import play.api.mvc.{Filter, Headers, RequestHeader, Result}

import scala.concurrent.Future
import scala.util.Failure

/**
  * Created by nishiyama on 2016/12/08.
  */
class ZipkinTraceFilter @Inject() (tracer: ZipkinTraceServiceLike)(implicit val mat: Materializer) extends Filter {

  import tracer.executionContext
  private val reqHeaderToSpanName: RequestHeader => String = ZipkinTraceFilter.ParamAwareRequestNamer

  def apply(nextFilter: (RequestHeader) => Future[Result])(req: RequestHeader): Future[Result] = {
    tracer.serverReceived(
      traceName = reqHeaderToSpanName(req),
      span = tracer.toSpan(req.headers)((headers, key) => headers.get(key))
    ).flatMap { serverSpan =>
      val result = nextFilter(req.copy(headers = new Headers(
        (req.headers.toMap.mapValues(_.headOption getOrElse "") ++ tracer.toMap(serverSpan)).toSeq
      )))
      result.onComplete {
        case Failure(t) => tracer.serverSend(serverSpan, "failed" -> s"Finished with exception: ${t.getMessage}")
        case _ => tracer.serverSend(serverSpan)
      }
      result
    }
  }
}

object ZipkinTraceFilter {
  val ParamAwareRequestNamer: RequestHeader => String = { reqHeader =>
    import org.apache.commons.lang3.StringUtils
    val tags = reqHeader.tags
    val pathPattern = StringUtils.replace(tags.getOrElse(play.api.routing.Router.Tags.RoutePattern, reqHeader.path), "<[^/]+>", "")
    s"${reqHeader.method} - $pathPattern"
  }
}
