package controllers

import java.util.concurrent.TimeUnit
import javax.inject.Named

import com.google.inject.Inject
import jp.co.bizreach.trace.play26.AkkaSupport._
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import services.ApiSampleService
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}
import jp.co.bizreach.trace.{TraceData, ZipkinTraceServiceLike}
import jp.co.bizreach.trace.play26.implicits.ZipkinTraceImplicits

/**
  * Created by nishiyama on 2016/12/05.
  */
class IndexController @Inject() (
  @Named("hello-actor") helloActor: ActorRef,
  components: ControllerComponents,
  service: ApiSampleService
) (
  implicit ec: ExecutionContext,
  val tracer: ZipkinTraceServiceLike
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

    implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)
    val f1 = TraceableActorRef(helloActor) ? HelloActorMessage("This is an actor call!")
    val f2 = service.sample("http://localhost:9992/api/nest")

    for {
      r1 <- f1
      r2 <- f2
    } yield Ok(Json.obj("result" -> (r1 + " " + r2)))
  }
}

case class HelloActorMessage(message: String)(implicit val traceData: ActorTraceData) extends TraceMessage

class HelloActor @Inject()(@Named("child-hello-actor") child: ActorRef)
                          (implicit val tracer: ZipkinTraceServiceLike) extends TraceableActor {
  def receive = {
    case m: HelloActorMessage => {
      Thread.sleep(1000)
      println(m.message)
      TraceableActorRef(child) ! HelloActorMessage("This is a child actor call!")
      sender() ! "result"
    }
  }
}

class ChildHelloActor @Inject()(val tracer: ZipkinTraceServiceLike) extends TraceableActor {
  def receive = {
    case m: HelloActorMessage => {
      Thread.sleep(1000)
      println(m.message)
    }
  }
}