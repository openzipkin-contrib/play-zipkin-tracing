package jp.co.bizreach.trace.play23.filter

import jp.co.bizreach.trace.play23.impl.ZipkinTraceService
import com.twitter.zipkin.gen.Span
import jp.co.bizreach.trace.play23.impl.ZipkinTraceService
import jp.co.bizreach.trace.play23.implicits.ZipkinRequestImplicits
import play.api.mvc.{Filter, Headers, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure
import scala.util.control.NonFatal

/**
  * Created by nishiyama on 2016/12/09.
  */
class ZipkinTraceFilter extends Filter with ZipkinRequestImplicits {

  private implicit lazy val zipkinService = ZipkinTraceService.service
  private implicit lazy val ec: ExecutionContext = zipkinService.eCtx
  private val reqHeaderToSpanName: RequestHeader => String = ZipkinTraceFilter.ParamAwareRequestNamer

  def apply(nextFilter: (RequestHeader) => Future[Result])(req: RequestHeader): Future[Result] = {
    val parentSpan = zipkinService.generateSpan(reqHeaderToSpanName(req), req2span(req))
    val fMaybeServerSpan = zipkinService.serverReceived(parentSpan).recover { case NonFatal(e) => None }
    fMaybeServerSpan flatMap {
      case None => nextFilter(req)
      case Some(serverSpan) => {
        val fResult = nextFilter(addHeadersToReq(req, zipkinService.serverSpanToSpan(serverSpan)))
        fResult.onComplete {
          case Failure(e) => zipkinService.serverSent(serverSpan, "failed" -> s"Finished with exception: ${e.getMessage}")
          case _ => zipkinService.serverSent(serverSpan)
        }
        fResult
      }
    }
  }

  private def addHeadersToReq(req: RequestHeader, span: Span): RequestHeader = {
    val originalHeaderData = req.headers.toMap
    val withSpanData = originalHeaderData ++ zipkinService.spanToIdsMap(span).map { case (key, value) => key -> Seq(value) }
    val newHeaders = new Headers{ override val data: Seq[(String, Seq[String])] = withSpanData.mapValues(_.headOption.getOrElse("")).toSeq.map{ case(k, v) => k -> Seq(v)} }
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
