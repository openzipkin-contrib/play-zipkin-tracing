package jp.co.bizreach.trace.play.module

import java.util.Collections

import brave.Tracing
import jp.co.bizreach.trace.ZipkinTraceServiceLike
import jp.co.bizreach.trace.play.ZipkinTraceService
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
      assert(Tracing.current() == null)

    }
    }
  }

  it should "provide a zipkin trace service" in {
    // TODO: dies due to missing dispatcher
    val service = injector.instanceOf[ZipkinTraceServiceLike]
    assert(service.isInstanceOf[ZipkinTraceService])
  }
}
