package jp.co.bizreach.trace.play26.filter

import javax.inject.Inject

import brave.Tracing
import brave.http.{HttpTracing, ITHttp, ITHttpServer}
import brave.propagation.ExtraFieldPropagation
import jp.co.bizreach.trace.ZipkinTraceServiceLike
import jp.co.bizreach.trace.play26.ZipkinTraceService
import org.junit.{After, AssumptionViolatedException}
import play.api.http.DefaultHttpFilters
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, Results}
import play.api.routing.Router.Routes
import play.api.routing.sird._
import play.api.routing.{Router, SimpleRouter}
import play.test._

import scala.concurrent.Future

class TestRouter @Inject()(
  val zipkin: ZipkinTraceServiceLike
) extends SimpleRouter {
  import zipkin.executionContext

  override def routes: Routes = {
    case GET(p"/foo") => Action {
      Results.Ok("bar")
    }

    case GET(p"/async") => Action.async {
      Future {
        // Call some blocking API
        Results.Ok("result of blocking call")
      }
    }

    case GET(p"/extra") => Action {
      Results.Ok(ExtraFieldPropagation.get(ITHttp.EXTRA_KEY))
    }

    case GET(p"/badrequest") => Action {
      Results.BadRequest
    }

    case GET(p"/child") => Action {
      zipkin.tracing.tracer.nextSpan.name("child").start.finish()
      Results.Ok("happy")
    }

    // approach borrowed from kamon.play.RequestHandlerInstrumentationSpec
    case GET(p"/exception") => Action {
      throw new Exception()
      Results.Ok("This page will generate an error!")
    }

    case GET(p"/exceptionAsync") => Action.async {
      Future {
        throw new Exception()
        Results.Ok("This page will generate an error!")
      }
    }
  }
}

class Filters @Inject() (
  zipkin: ZipkinTraceFilter
) extends DefaultHttpFilters(zipkin)

// Uses some hints from https://github.com/kamon-io/kamon-play/blob/master/kamon-play-2.6.x/src/test/scala/kamon/play/RequestInstrumentationSpec.scala
class ZipkinTraceFilterTest extends ITHttpServer {
  var testServer: TestServer = _

  override def init(): Unit = {
    val port = play.api.test.Helpers.testServerPort
    val app = new GuiceApplicationBuilder()
      .configure("play.http.filters" -> classOf[Filters].getName)
      .overrides(
        bind[Tracing].toInstance(httpTracing.tracing()),
        bind[HttpTracing].toInstance(httpTracing),
        bind[ZipkinTraceServiceLike].to[ZipkinTraceService],
        bind[Router].to[TestRouter]
      )
      .build()
    // prefer to use port 0 and then lookup the value it uses, but that doesn't work
    testServer = new TestServer(port, app.asJava)
    testServer.start()
  }

  @After def stop(): Unit = {
    if (testServer != null) testServer.stop()
  }

  override def readsExtra_existingTrace() =
    throw new AssumptionViolatedException("TODO: Request handlers cannot read Tracer.currentSpan")

  override def readsExtra_newTrace() =
    throw new AssumptionViolatedException("TODO: Request handlers cannot read Tracer.currentSpan")

  override def readsExtra_unsampled() =
    throw new AssumptionViolatedException("TODO: Request handlers cannot read Tracer.currentSpan")

  override def createsChildSpan() =
    throw new AssumptionViolatedException("TODO: Request handlers cannot read Tracer.currentSpan")

  override def url(path: String) = s"http://localhost:${testServer.port}${path}"
}

