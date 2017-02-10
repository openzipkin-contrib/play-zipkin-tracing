package repositories

import javax.inject.Inject

import jp.co.bizreach.trace.service.{TraceCassette, TraceServiceLike, TracedFuture}
import jp.co.bizreach.trace.play25.implicits.TraceImplicits._
import play.api.Logger
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by nishiyama on 2016/12/05.
  */
class ApiRepository @Inject() (ws :WSClient)(implicit val tracer: TraceServiceLike, ec: ExecutionContext) {

  def call(url: String)(implicit traceCassette: TraceCassette): Future[String] = {
    TracedFuture("zipkin-api-call"){ implicit cassette =>
      Logger.debug(cassette.toString)
      ws.url(url)
        .withTraceHeader()
        .get().map(_ => "OK")
    }
  }
}
