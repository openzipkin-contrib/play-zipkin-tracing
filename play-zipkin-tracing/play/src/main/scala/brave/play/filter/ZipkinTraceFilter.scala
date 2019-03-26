package brave.play.filter

import javax.inject.Inject

import akka.stream.Materializer
import brave.play.ZipkinTraceServiceLike
import play.api.mvc.{Filter, Headers, RequestHeader, Result}
import play.api.routing.Router

import scala.concurrent.Future
import scala.util.Failure

/**
 * A Zipkin filter.
 *
 * This filter is that reports how long a request takes to execute in Play as a server span.
 * The way to use this filter is following:
 * {{{
 * class Filters @Inject() (
 *   zipkin: ZipkinTraceFilter
 * ) extends DefaultHttpFilters(zipkin)
 * }}}
 *
 * @param tracer a Zipkin tracer
 * @param mat a materializer
 */
class ZipkinTraceFilter @Inject() (tracer: ZipkinTraceServiceLike)(implicit val mat: Materializer) extends Filter {

  import tracer.executionContext
  private val reqHeaderToSpanName: RequestHeader => String = ZipkinTraceFilter.ParamAwareRequestNamer

  def apply(nextFilter: RequestHeader => Future[Result])(req: RequestHeader): Future[Result] = {
    val serverSpan = tracer.serverReceived(
      spanName = reqHeaderToSpanName(req),
      span = tracer.newSpan(req.headers)((headers, key) => headers.get(key))
    )
    val result = nextFilter(req.withHeaders(new Headers(
      (req.headers.toMap.mapValues(_.headOption getOrElse "") ++ tracer.toMap(serverSpan)).toSeq
    )))
    result.onComplete {
      case Failure(t) => tracer.serverSend(serverSpan, "failed" -> s"Finished with exception: ${t.getMessage}")
      case _ => tracer.serverSend(serverSpan)
    }
    result
  }
}

object ZipkinTraceFilter {
  val ParamAwareRequestNamer: RequestHeader => String = { reqHeader =>
    import org.apache.commons.lang3.StringUtils
    val pathPattern = StringUtils.replace(
      reqHeader.attrs.get(Router.Attrs.HandlerDef).map(_.path).getOrElse(reqHeader.path),
      "<[^/]+>", ""
    )
    s"${reqHeader.method} - $pathPattern"
  }
}
