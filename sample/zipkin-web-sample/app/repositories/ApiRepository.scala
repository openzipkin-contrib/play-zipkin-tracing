package repositories

import javax.inject.Inject

import jp.co.bizreach.trace.TraceCassette
import jp.co.bizreach.trace.play25.ZipkinTraceService
import jp.co.bizreach.trace.play25.implicits.ZipkinTraceImplicits
import play.api.Logger
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by nishiyama on 2016/12/05.
  */
class ApiRepository @Inject() (
  ws :WSClient,
  val tracer: ZipkinTraceService
)(implicit val ec: ExecutionContext) extends ZipkinTraceImplicits {

  def call(url: String)(implicit traceCassette: TraceCassette): Future[String] = {
    tracer.traceFuture("zipkin-api-call"){ cassette =>
      Logger.debug(cassette.toString)
      ws.url(url)
        .withTraceHeader()
        .get().map(_ => "OK")
    }
  }
}
