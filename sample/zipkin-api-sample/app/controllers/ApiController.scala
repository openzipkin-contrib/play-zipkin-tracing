package controllers

import javax.inject.Inject

import com.stanby.trace.service.{TraceServiceLike, TracedFuture}
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

/**
  * Created by nishiyama on 2016/12/05.
  */
class ApiController @Inject() (ws: WSClient)(implicit val traceService: TraceServiceLike, implicit val ec: ExecutionContext) extends Controller {

  def once = Action.async { implicit req =>
    Logger.debug(req.headers.toSimpleMap.map{ case (k, v) => s"${k}:${v}"}.toSeq.mkString("\n"))
    Future.successful(Ok(Json.obj("api" -> "once")))
  }

  def onceWait = Action.async { implicit req =>
    Logger.debug(req.headers.toSimpleMap.map{ case (k, v) => s"${k}:${v}"}.toSeq.mkString("\n"))

    Future.successful{
      Ok(Json.obj("wait" -> "aa"))
    }
  }

  def nest = Action.async { implicit req =>
    import com.stanby.trace.play25.implicits.TraceImplicits._
    val waited = Random.nextInt(900)
    Thread.sleep(waited + 100)

    TracedFuture("zipkin-api-nest-call"){ implicit cassette =>
      ws.url("http://localhost:9992/api/once")
        .withTraceHeader()
        .get().map{ res =>
        Ok(res.json)
      }
    }
  }
}
