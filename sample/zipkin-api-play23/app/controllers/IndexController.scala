package controllers

import jp.co.bizreach.trace.play23.TraceWS
import jp.co.bizreach.trace.play23.implicits.ZipkinTraceImplicits
import play.api.mvc.{Action, Controller}
import play.api.Play.current

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by nishiyama on 2016/12/09.
  */
object IndexController extends Controller with ZipkinTraceImplicits {

  def sample = Action.async{ implicit request =>
    TraceWS.url("play23-api-call", "http://localhost:9992/api/once")
      .get().map { res => Ok(res.json) }
  }
}
