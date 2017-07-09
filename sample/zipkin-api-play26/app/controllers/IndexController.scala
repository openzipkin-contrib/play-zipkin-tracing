package controllers

import javax.inject.Named

import com.google.inject.Inject
import jp.co.bizreach.trace.play26.AkkaSupport.{TraceMessage, ZipkinTraceActor}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import services.ApiSampleService

import akka.actor._
import scala.concurrent.{ExecutionContext, Future}
import jp.co.bizreach.trace.{TraceData, ZipkinTraceServiceLike}
import jp.co.bizreach.trace.play26.implicits.ZipkinTraceImplicits

/**
  * Created by nishiyama on 2016/12/05.
  */
class IndexController @Inject() (
  @Named("hello-actor") helloActor: ActorRef,
  components: ControllerComponents,
  service: ApiSampleService,
  val tracer: ZipkinTraceServiceLike
) (
  implicit ec: ExecutionContext
) extends AbstractController(components) with ZipkinTraceImplicits {

  def index = Action.async { implicit req =>
    Future.successful(Ok(Json.obj("status" -> "ok")))
  }

  def once = Action.async { implicit req =>
    Logger.debug(req.headers.toSimpleMap.map{ case (k, v) => s"${k}:${v}"}.toSeq.mkString("\n"))

    service.sample("http://localhost:9992/api/once").map(_ => Ok(Json.obj("OK"->"OK")))
  }

  def nest = Action.async { implicit req =>
    Logger.debug(req.headers.toSimpleMap.map{ case (k, v) => s"${k}:${v}"}.toSeq.mkString("\n"))

    helloActor ! HelloActorMessage("This is an actor call!")

    service.sample("http://localhost:9992/api/nest").map(v => Ok(Json.obj("result" -> v)))
  }
}

case class HelloActorMessage(message: String)(implicit val traceData: TraceData) extends TraceMessage

class HelloActor @Inject()(@Named("child-hello-actor") child: ActorRef, val tracer: ZipkinTraceServiceLike) extends ZipkinTraceActor {
  def receive = {
    case m: HelloActorMessage => {
      Thread.sleep(1000)
      println(m.message)
      child ! HelloActorMessage("This is a child actor call!")
    }
  }
}

class ChildHelloActor @Inject()(val tracer: ZipkinTraceServiceLike) extends ZipkinTraceActor {
  def receive = {
    case m: HelloActorMessage => {
      Thread.sleep(1000)
      println(m.message)
    }
  }
}