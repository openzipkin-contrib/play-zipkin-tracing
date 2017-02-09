package controllers

import com.stanby.trace.play23.impl.ZipkinTraceService
import com.stanby.trace.service.TracedFuture
import play.api.libs.ws.WS
import play.api.mvc.{Action, Controller}
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by nishiyama on 2016/12/09.
  */
object IndexController extends Controller {
  import com.stanby.trace.play23.implicits.TraceImplicits._
  implicit val traceService = ZipkinTraceService
  def sample = Action.async{ implicit request =>

    TracedFuture("play23-api-call"){ implicit c =>
      WS.url("http://localhost:9992/api/once")
        .withTraceHeader()
        .get().map{ res =>
        Ok(res.json)
      }

    }
  }
}
