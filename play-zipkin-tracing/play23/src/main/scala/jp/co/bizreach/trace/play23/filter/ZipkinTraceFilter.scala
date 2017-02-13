package jp.co.bizreach.trace.play23.filter

import jp.co.bizreach.trace.play23.impl.ZipkinTraceService
import jp.co.bizreach.trace.play23.implicits.ZipkinRequestImplicits
import jp.co.bizreach.trace.service.zipkin.ZipkinTraceCassette
import play.api.mvc.{Filter, Headers, RequestHeader, Result}

import scala.concurrent.Future
import scala.util.Failure
import scala.util.control.NonFatal

/**
  * Created by nishiyama on 2016/12/09.
  */
class ZipkinTraceFilter extends Filter with ZipkinRequestImplicits {

  import ZipkinTraceService.executionContext
  private val reqHeaderToSpanName: RequestHeader => String = ZipkinTraceFilter.ParamAwareRequestNamer

  def apply(nextFilter: (RequestHeader) => Future[Result])(req: RequestHeader): Future[Result] = {
    val parentCassette = ZipkinTraceService.generateTrace(reqHeaderToSpanName(req), req2trace(req))

    ZipkinTraceService.serverReceived(parentCassette)
      .recover { case NonFatal(e) => None }
      .flatMap {
        case None => nextFilter(req)
        case Some(serverSpan) =>
          val result = nextFilter(addHeadersToReq(req, parentCassette))
          result.onComplete {
            case Failure(t) => ZipkinTraceService.serverSent(parentCassette, serverSpan, "failed" -> s"Finished with exception: ${t.getMessage}")
            case _ => ZipkinTraceService.serverSent(parentCassette, serverSpan)
          }
          result
      }
  }

  private[filter] def addHeadersToReq(req: RequestHeader, cassette: ZipkinTraceCassette): RequestHeader = {
    val originalHeaderData = req.headers.toMap
    val withCassetteData = originalHeaderData ++ ZipkinTraceService.cassetteToMap(cassette).map { case (key, value) => key -> Seq(value) }
    val newHeaders = new Headers{ override val data: Seq[(String, Seq[String])] = withCassetteData.mapValues(_.headOption.map(Seq(_)).getOrElse(Seq(""))).toSeq }
    req.copy(headers = newHeaders)
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
