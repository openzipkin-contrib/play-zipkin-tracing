package controllers

import java.util.concurrent.TimeUnit
import javax.inject.Named

import akka.actor._
import akka.util.Timeout
import brave.play.ZipkinTraceServiceLike
import brave.play.actor.ActorTraceSupport._
import brave.play.implicits.ZipkinTraceImplicits
import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc._
import services.ApiSampleService

import scala.concurrent.{ExecutionContext, Future}

class IndexController @Inject() (
  @Named("hello-actor") helloActor: ActorRef,
  components: ControllerComponents,
  service: ApiSampleService
) (
  implicit ec: ExecutionContext,
  val tracer: ZipkinTraceServiceLike
) extends AbstractController(components) with Logging with ZipkinTraceImplicits {

  def index = Action.async {
    Future.successful(Ok(Json.obj("status" -> "ok")))
  }

  def once = Action.async { implicit req: Request[_] =>
    logger.debug(req.headers.toSimpleMap.map{ case (k, v) => s"${k}:${v}"}.toSeq.mkString("\n"))

    service.sample("http://localhost:9992/api/once").map(_ => Ok(Json.obj("OK"->"OK")))
  }

  def nest = Action.async { implicit req: Request[_] =>
    logger.debug(req.headers.toSimpleMap.map{ case (k, v) => s"${k}:${v}"}.toSeq.mkString("\n"))

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
