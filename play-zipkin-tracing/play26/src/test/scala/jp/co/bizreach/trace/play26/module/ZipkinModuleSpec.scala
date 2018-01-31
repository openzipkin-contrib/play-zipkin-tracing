package jp.co.bizreach.trace.play26.module

import java.util.Collections

import brave.Tracing
import brave.http.HttpTracing
import jp.co.bizreach.trace.ZipkinTraceServiceLike
import jp.co.bizreach.trace.play26.ZipkinTraceService
import org.scalatest.AsyncFlatSpec
import play.api.inject.ApplicationLifecycle
import play.api.inject.guice.GuiceApplicationBuilder
import zipkin2.reporter.Sender
import zipkin2.reporter.okhttp3.OkHttpSender

class ZipkinModuleSpec extends AsyncFlatSpec {
  val injector = new GuiceApplicationBuilder()
    .bindings(new ZipkinModule)
    .injector()

  it should "provide an okhttp sender" in {
    val sender = injector.instanceOf[Sender]
    assert(sender.isInstanceOf[OkHttpSender])
  }

  it should "eventually close the sender" in {
    // provisioning the sender so we can tell if it is closed on shutdown
    val sender = injector.instanceOf[Sender]

    // stopping the application should close the sender!
    injector.instanceOf[ApplicationLifecycle].stop map { _ => {
      val thrown = intercept[Exception] {
        sender.sendSpans(Collections.emptyList[Array[Byte]]).execute()
      }
      assert(thrown.getMessage === "closed")
    }
    }
  }

  it should "provide a tracing component" in {
    val tracing = injector.instanceOf[Tracing]
    assert(Tracing.current() != null)
  }

  it should "eventually close the tracing component" in {
    // provisioning the tracing component so we can tell if it is closed on shutdown
    val tracing = injector.instanceOf[Tracing]

    // stopping the application should close the tracing component!
    injector.instanceOf[ApplicationLifecycle].stop map { _ => {
      val currentTracing = Tracing.current()
      assert(currentTracing == null || currentTracing != tracing)

    }
    }
  }

  it should "provide an http tracing component" in {
    val httpTracing = injector.instanceOf[HttpTracing]
    assert(httpTracing.tracing() == injector.instanceOf[Tracing])
  }

  it should "provide a zipkin trace service" in {
    val service = injector.instanceOf[ZipkinTraceServiceLike]
    assert(service.isInstanceOf[ZipkinTraceService])

    assert(service.tracing == injector.instanceOf[Tracing])
    assert(service.httpTracing == injector.instanceOf[HttpTracing])
  }
}
