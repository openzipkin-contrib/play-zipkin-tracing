package repositories

import javax.inject.Inject

import brave.play.{TraceData, TraceWSClient, ZipkinTraceServiceLike}
import brave.play.implicits.ZipkinTraceImplicits
import play.api.Logging

import scala.concurrent.{ExecutionContext, Future}

class ApiRepository @Inject() (
  ws :TraceWSClient,
  val tracer: ZipkinTraceServiceLike
)(implicit val ec: ExecutionContext) extends Logging with ZipkinTraceImplicits {

  def call(url: String)(implicit traceData: TraceData): Future[String] = {
    logger.debug(traceData.toString)
    ws.url("zipkin-api-call", url).get().map(_ => "OK")
  }
}
