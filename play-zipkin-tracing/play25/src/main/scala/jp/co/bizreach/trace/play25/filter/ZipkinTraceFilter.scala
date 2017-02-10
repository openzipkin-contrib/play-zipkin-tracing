package jp.co.bizreach.trace.play25.filter

import javax.inject.Inject

import akka.stream.Materializer
import play.api.mvc.{Filter, Headers, RequestHeader, Result}
import jp.co.bizreach.trace.play25.implicits.ZipkinRequestImplicits
import jp.co.bizreach.trace.service.zipkin.ZipkinTraceCassette
import com.twitter.zipkin.gen.Span
import jp.co.bizreach.trace.play25.implicits.ZipkinRequestImplicits
import jp.co.bizreach.trace.service.zipkin.ZipkinTraceServiceLike

import scala.concurrent.Future
import scala.util.{Failure, Try}
import scala.util.control.NonFatal

/**
  * Created by nishiyama on 2016/12/08.
  */
class ZipkinTraceFilter @Inject() (zipkinTracer: ZipkinTraceServiceLike)(implicit val mat: Materializer) extends Filter with ZipkinRequestImplicits {
  import play.api.libs.concurrent.Execution.Implicits._

  private implicit lazy val zipkinService = zipkinTracer.service
  private val reqHeaderToSpanName: RequestHeader => String = ZipkinTraceFilter.ParamAwareRequestNamer

  def apply(nextFilter: (RequestHeader) => Future[Result])(req: RequestHeader): Future[Result] = {
    Try(zipkinTracer.generateTrace(reqHeaderToSpanName(req), req2trace(req))).toOption.fold {
      nextFilter(req)
    }{ parentCassette =>
      val fMaybeServerSpan = zipkinTracer.serverReceived(parentCassette).recover{ case NonFatal(e) => None }

      fMaybeServerSpan flatMap {
        case None => nextFilter(req)
        case Some(serverSpan) =>
          val fResult = nextFilter(addHeadersToReq(req, parentCassette))
          fResult.onComplete {
            case Failure(e) => zipkinTracer.serverSent(parentCassette, serverSpan, "failed" -> s"Finished with exception: ${e.getMessage}")
            case _ => zipkinTracer.serverSent(parentCassette, serverSpan)
          }
          fResult
      }
    }
  }

  private def addHeadersToReq(req: RequestHeader, cassette: ZipkinTraceCassette): RequestHeader = {
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