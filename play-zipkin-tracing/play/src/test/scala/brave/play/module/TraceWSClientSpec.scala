package brave.play.module

import brave.play.{TestZipkinTraceService, TraceWSClient, ZipkinTraceServiceLike}
import mockws.MockWS
import org.scalatest.{AsyncFlatSpec, Matchers}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import brave.play

class TraceWSClientSpec extends AsyncFlatSpec with Matchers {
  val zipkinService = new TestZipkinTraceService

  val injector = new GuiceApplicationBuilder()
    .bindings(bind[ZipkinTraceServiceLike].to(zipkinService))
    .overrides(bind[WSClient].to(MockWS(PartialFunction.empty)))
    .injector

  val client = injector.instanceOf[TraceWSClient]

  it should "set tags in span" in {
    implicit val parentTraceData = play.TraceData(zipkinService.toSpan(Map.empty)((_, _) => None))

    val result = client.url("test-span", "http://localhost/testpath").execute()

    result.map { _ =>
      parentTraceData.span.finish()
      val span = zipkinService.reporter.spans.find(_.name() == "test-span")

      span shouldNot be(empty)
      span.map(_.tags().get("http.method")) should contain("GET")
      span.map(_.tags().get("http.host")) should contain("localhost")
      span.map(_.tags().get("http.path")) should contain("/testpath")
    }
  }
}
