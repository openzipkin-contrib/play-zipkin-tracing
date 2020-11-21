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

import brave.Tracing
import brave.play.{ZipkinTraceConfig, ZipkinTraceService, ZipkinTraceServiceLike}
import brave.sampler.Sampler
import javax.inject.{Inject, Provider}
import play.api.Configuration
import play.api.inject.{ApplicationLifecycle, SimpleModule, bind}
import zipkin2.reporter.okhttp3.OkHttpSender
import zipkin2.reporter.{AsyncReporter, Sender}

import scala.concurrent.Future

/**
  * A Zipkin module.
  *
  * This module can be registered with Play automatically by appending it in application.conf:
  * {{{
  *   play.modules.enabled += "brave.play.module.ZipkinModule"
  * }}}
  *
  */
class ZipkinModule extends SimpleModule((env, conf) =>
  Seq(
    bind[Sender].toProvider(classOf[SenderProvider]),
    bind[Tracing].toProvider(classOf[TracingProvider]),
    bind[ZipkinTraceServiceLike].to[ZipkinTraceService]
  )
)

class SenderProvider @Inject()(conf: Configuration, lifecycle: ApplicationLifecycle) extends Provider[Sender] {
  override def get(): Sender = {
    val baseUrl = conf.getOptional[String](ZipkinTraceConfig.ZipkinBaseUrl) getOrElse "http://localhost:9411"
    val result = OkHttpSender.create(baseUrl + "/api/v2/spans")
    lifecycle.addStopHook(() => Future.successful(result.close()))
    result
  }
}

class TracingProvider @Inject()(sender: Provider[Sender],
                                conf: Configuration,
                                lifecycle: ApplicationLifecycle)
  extends Provider[Tracing] {

  override def get(): Tracing = {
    // not injecting a span reporter, as you can't bind parameterized types like
    // Reporter[Span] here per https://github.com/playframework/playframework/issues/3422
    val spanReporter = AsyncReporter.create(sender.get())
    lifecycle.addStopHook(() => Future.successful(spanReporter.close()))
    val result = Tracing.newBuilder()
      .localServiceName(conf.getOptional[String](ZipkinTraceConfig.ServiceName) getOrElse "unknown")
      .spanReporter(spanReporter)
      .sampler(conf.getOptional[String](ZipkinTraceConfig.ZipkinSampleRate)
        .map(s => Sampler.create(s.toFloat)) getOrElse Sampler.ALWAYS_SAMPLE
      )
      .build()
    lifecycle.addStopHook(() => Future.successful(result.close()))
    result
  }
}
