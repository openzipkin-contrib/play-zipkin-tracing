package brave.play.module

import java.util.Collections

import akka.actor.CoordinatedShutdown
import brave.Tracing
import brave.play.{ZipkinTraceService, ZipkinTraceServiceLike}
import org.scalatest.AsyncFlatSpec
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
    injector.instanceOf[CoordinatedShutdown].run(CoordinatedShutdown.UnknownReason) map { _ =>
      val thrown = intercept[Exception] {
        sender.sendSpans(Collections.emptyList[Array[Byte]]).execute()
      }
      assert(thrown.getMessage === "closed")
    }
  }

  it should "provide a tracing component" in instanceOfTracing { tracing =>
    assert(Tracing.current() != null)
    assert(Tracing.current() == tracing)
  }

  it should "eventually close the tracing component" in instanceOfTracing { tracing =>
    // stopping the application should close the tracing component!
    injector.instanceOf[CoordinatedShutdown].run(CoordinatedShutdown.UnknownReason) map { _ =>
      assert(Tracing.current() == null)
    }
  }

  private def instanceOfTracing[A](test: Tracing => A): A = {
    val tracing = injector.instanceOf[Tracing]
    try {
      test(tracing)
    } finally {
      // Ensures there is no active Tracing object
      tracing.close()
    }
  }

  it should "provide a zipkin trace service" in {
    // TODO: dies due to missing dispatcher
    val service = injector.instanceOf[ZipkinTraceServiceLike]
    assert(service.isInstanceOf[ZipkinTraceService])
  }
}
