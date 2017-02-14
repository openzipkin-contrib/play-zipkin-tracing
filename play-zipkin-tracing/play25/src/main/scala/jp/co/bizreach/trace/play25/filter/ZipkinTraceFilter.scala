package jp.co.bizreach.trace.play25.filter

import javax.inject.Inject

import akka.stream.Materializer
import jp.co.bizreach.trace.play25.implicits.ZipkinRequestImplicits
import jp.co.bizreach.trace.service.zipkin.ZipkinTraceCassette
import jp.co.bizreach.trace.service.zipkin.ZipkinTraceServiceLike
import play.api.mvc.{Filter, Headers, RequestHeader, Result}

import scala.concurrent.Future
import scala.util.Failure
import scala.util.control.NonFatal

/**
  * Created by nishiyama on 2016/12/08.
  */
class ZipkinTraceFilter @Inject() (zipkinTracer: ZipkinTraceServiceLike)(implicit val mat: Materializer) extends Filter with ZipkinRequestImplicits {

  import zipkinTracer.executionContext
  private val reqHeaderToSpanName: RequestHeader => String = ZipkinTraceFilter.ParamAwareRequestNamer

  def apply(nextFilter: (RequestHeader) => Future[Result])(req: RequestHeader): Future[Result] = {
    val parentCassette = zipkinTracer.generateTrace(reqHeaderToSpanName(req), req2trace(req))

    zipkinTracer.serverReceived(parentCassette)
      .recover { case NonFatal(e) => None }
      .flatMap {
        case None => nextFilter(req)
        case Some(serverSpan) =>
          val result = nextFilter(addHeadersToReq(req, parentCassette))
          result.onComplete {
            case Failure(t) => zipkinTracer.serverSend(parentCassette, serverSpan, "failed" -> s"Finished with exception: ${t.getMessage}")
            case _ => zipkinTracer.serverSend(parentCassette, serverSpan)
          }
          result
      }
  }

  private[filter] def addHeadersToReq(req: RequestHeader, cassette: ZipkinTraceCassette): RequestHeader = {
    val originalHeaderData = req.headers.toMap
    val withCassetteData = originalHeaderData ++ zipkinTracer.cassetteToMap(cassette).map { case (key, value) => key -> Seq(value) }
    val newHeaders = new Headers(withCassetteData.mapValues(_.headOption.getOrElse("")).toSeq)
    req.copy(headers = newHeaders)
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
