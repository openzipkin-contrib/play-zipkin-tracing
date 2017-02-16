package controllers

import jp.co.bizreach.trace.play23.ZipkinTraceService
import jp.co.bizreach.trace.play23.implicits.ZipkinTraceImplicits
import play.api.libs.ws.WS
import play.api.mvc.{Action, Controller}
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by nishiyama on 2016/12/09.
  */
object IndexController extends Controller with ZipkinTraceImplicits {

  implicit val tracer = ZipkinTraceService

  def sample = Action.async{ implicit request =>
    tracer.traceFuture("play23-api-call"){ c =>
      WS.url("http://localhost:9992/api/once")
        .withTraceHeader()
        .get().map{ res =>
        Ok(res.json)
      }
    }
  }
}
