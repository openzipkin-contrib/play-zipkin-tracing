/**
 * Copyright 2017-2020 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
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
