package controllers

import com.google.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import services.ApiSampleService

import scala.concurrent.{ExecutionContext, Future}
import jp.co.bizreach.trace.ZipkinTraceServiceLike
import jp.co.bizreach.trace.play25.implicits.ZipkinTraceImplicits

/**
  * Created by nishiyama on 2016/12/05.
  */
class IndexController @Inject() (
  service: ApiSampleService,
  val tracer: ZipkinTraceServiceLike
) (
  implicit ec: ExecutionContext
) extends Controller with ZipkinTraceImplicits {

  def index = Action.async { implicit req =>
    Future.successful(Ok(Json.obj("status" -> "ok")))
  }

  def once = Action.async { implicit req =>
    Logger.debug(req.headers.toSimpleMap.map{ case (k, v) => s"${k}:${v}"}.toSeq.mkString("\n"))

    service.sample("http://localhost:9992/api/once").map(_ => Ok(Json.obj("OK"->"OK")))
  }

  def nest = Action.async { implicit req =>
    Logger.debug(req.headers.toSimpleMap.map{ case (k, v) => s"${k}:${v}"}.toSeq.mkString("\n"))

    service.sample("http://localhost:9992/api/nest").map(v => Ok(Json.obj("result" -> v)))
  }
}
