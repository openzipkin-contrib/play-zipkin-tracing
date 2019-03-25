package zipkin.play.module

import javax.inject.{Inject, Provider}

import brave.Tracing
import brave.sampler.Sampler
import play.api.Configuration
import play.api.inject.{ApplicationLifecycle, SimpleModule, bind}
import zipkin.play.{ZipkinTraceConfig, ZipkinTraceService, ZipkinTraceServiceLike}
import zipkin2.reporter.okhttp3.OkHttpSender
import zipkin2.reporter.{AsyncReporter, Sender}

import scala.concurrent.Future

/**
  * A Zipkin module.
  *
  * This module can be registered with Play automatically by appending it in application.conf:
  * {{{
  *   play.modules.enabled += "zipkin.play.module.ZipkinModule"
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
