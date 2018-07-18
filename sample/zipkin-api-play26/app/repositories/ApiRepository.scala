package repositories

import javax.inject.Inject

import jp.co.bizreach.trace.play.TraceWSClient
import jp.co.bizreach.trace.{TraceData, ZipkinTraceServiceLike}
import jp.co.bizreach.trace.play.implicits.ZipkinTraceImplicits
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by nishiyama on 2016/12/05.
  */
class ApiRepository @Inject() (
  ws :TraceWSClient,
  val tracer: ZipkinTraceServiceLike
)(implicit val ec: ExecutionContext) extends ZipkinTraceImplicits {

  def call(url: String)(implicit traceData: TraceData): Future[String] = {
    Logger.debug(traceData.toString)
    ws.url("zipkin-api-call", url).get().map(_ => "OK")
  }
}
