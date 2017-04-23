package jp.co.bizreach.trace.play23

import brave.Tracer
import brave.sampler.Sampler
import jp.co.bizreach.trace._
import play.api.{Play, Configuration}
import play.api.libs.concurrent.Akka
import zipkin.reporter.AsyncReporter
import zipkin.reporter.okhttp3.OkHttpSender

import scala.concurrent.ExecutionContext

/**
 * Object for Zipkin tracing at Play2.3.
 */
object ZipkinTraceService extends ZipkinTraceServiceLike {
  import play.api.Play.current
  val conf: Configuration = Play.configuration

  implicit val executionContext: ExecutionContext = Akka.system.dispatchers.lookup(ZipkinTraceConfig.AkkaName)

  val tracer = Tracer.newBuilder()
    .localServiceName(conf.getString(ZipkinTraceConfig.ServiceName) getOrElse "example")
    .reporter(AsyncReporter
      .builder(OkHttpSender.create(
        s"${conf.getString(ZipkinTraceConfig.ZipkinProtocol) getOrElse "http"}://${conf.getString(ZipkinTraceConfig.ZipkinHost) getOrElse "localhost"}:${conf.getInt(ZipkinTraceConfig.ZipkinPort) getOrElse 9411}/api/v1/spans"
      ))
      .build()
    )
    .sampler(conf.getString(ZipkinTraceConfig.ZipkinSampleRate)
      .map(s => Sampler.create(s.toFloat)) getOrElse Sampler.ALWAYS_SAMPLE
    )
    .build()

}
