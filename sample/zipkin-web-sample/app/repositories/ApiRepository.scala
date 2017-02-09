package repositories

import javax.inject.Inject

import com.stanby.trace.service.{TraceCassette, TraceServiceLike, TracedFuture}
import play.api.Logger
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by nishiyama on 2016/12/05.
  */
class ApiRepository @Inject() (ws :WSClient)(implicit val tracer: TraceServiceLike, ec: ExecutionContext) {
  import com.stanby.trace.play25.implicits.TraceImplicits._

  def call(url: String)(implicit traceCassette: TraceCassette): Future[String] = {
    TracedFuture("zipkin-api-call"){ implicit cassette =>
      Logger.debug(cassette.toString)
      ws.url(url)
        .withTraceHeader()
        .get().map(_ => "OK")
    }
  }
}
