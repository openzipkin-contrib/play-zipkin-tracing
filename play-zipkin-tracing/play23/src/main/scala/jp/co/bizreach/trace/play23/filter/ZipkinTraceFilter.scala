package jp.co.bizreach.trace.play23.filter

import jp.co.bizreach.trace.play23.ZipkinTraceService
import play.api.mvc.{Filter, Headers, RequestHeader, Result}

import scala.concurrent.Future
import scala.util.Failure

/**
 * A Zipkin filter.
 *
 * This filter is that reports how long a request takes to execute in Play as a server span.
 * The way to use this filter is following:
 * {{{
 * object Global extends WithFilters(new ZipkinTraceFilter()) with GlobalSettings
 * }}}
 *
 */
class ZipkinTraceFilter extends Filter {

  import ZipkinTraceService.executionContext
  private val reqHeaderToSpanName: RequestHeader => String = ZipkinTraceFilter.ParamAwareRequestNamer

  def apply(nextFilter: (RequestHeader) => Future[Result])(req: RequestHeader): Future[Result] = {
    val serverSpan = ZipkinTraceService.serverReceived(
      spanName = reqHeaderToSpanName(req),
      span = ZipkinTraceService.newSpan(req.headers)((headers, key) => headers.get(key))
    )
    val result = nextFilter(req.copy(headers = new Headers {
      protected val data: Seq[(String, Seq[String])] = {
        req.headers.toMap ++ ZipkinTraceService.toMap(serverSpan).map { case (key, value) => key -> Seq(value) }
      }.toSeq
    }))
    result.onComplete {
      case Failure(t) => ZipkinTraceService.serverSend(serverSpan, "failed" -> s"Finished with exception: ${t.getMessage}")
      case _ => ZipkinTraceService.serverSend(serverSpan)
    }
    result
  }
}

object ZipkinTraceFilter {
  val ParamAwareRequestNamer: RequestHeader => String = { reqHeader =>
    import org.apache.commons.lang3.StringUtils
    val tags = reqHeader.tags
    val pathPattern = StringUtils.replace(tags.getOrElse(play.api.Routes.ROUTE_PATTERN, reqHeader.path), "<[^/]+>", "")
    s"${reqHeader.method} - $pathPattern"
  }
}
