package jp.co.bizreach.trace.play26.module

import javax.inject.{Inject, Provider, Singleton}

import brave.Tracing
import brave.http.HttpTracing
import brave.sampler.Sampler
import jp.co.bizreach.trace.play26.ZipkinTraceService
import jp.co.bizreach.trace.{ZipkinTraceConfig, ZipkinTraceServiceLike}
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
  *   play.modules.enabled += "jp.co.bizreach.trace.play26.module.ZipkinModule"
  * }}}
  *
  */
class ZipkinModule extends SimpleModule((env, conf) =>
  Seq(
    bind[Sender].toProvider(classOf[SenderProvider]).in(classOf[Singleton]),
    bind[Tracing].toProvider(classOf[TracingProvider]).in(classOf[Singleton]),
    bind[HttpTracing].toProvider(classOf[HttpTracingProvider]).in(classOf[Singleton]),
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

class HttpTracingProvider @Inject()(sender: Provider[Tracing],
                                    conf: Configuration,
                                    lifecycle: ApplicationLifecycle)
  extends Provider[HttpTracing] {

  override def get(): HttpTracing = {
    HttpTracing.create(sender.get())
  }
}
