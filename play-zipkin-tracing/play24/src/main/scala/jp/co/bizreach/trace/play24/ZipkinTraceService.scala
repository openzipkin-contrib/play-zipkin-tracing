package jp.co.bizreach.trace.play24

import javax.inject.Inject

import akka.actor.ActorSystem
import brave.Tracer
import brave.sampler.Sampler
import jp.co.bizreach.trace.{ZipkinTraceServiceLike, ZipkinTraceConfig}
import play.api.Configuration
import zipkin.reporter.AsyncReporter
import zipkin.reporter.okhttp3.OkHttpSender

import scala.concurrent.ExecutionContext

/**
 * Class for Zipkin tracing at Play2.5.
 *
 * @param conf a Play's configuration
 * @param actorSystem a Play's actor system
 */
class ZipkinTraceService @Inject() (
  conf: Configuration,
  actorSystem: ActorSystem) extends ZipkinTraceServiceLike {

  implicit val executionContext: ExecutionContext = actorSystem.dispatchers.lookup(ZipkinTraceConfig.AkkaName)

  val tracer = Tracer.newBuilder()
    .localServiceName(conf.getString(ZipkinTraceConfig.ServiceName) getOrElse "example")
    .reporter(AsyncReporter
      .builder(OkHttpSender.create(
        s"http://${conf.getString(ZipkinTraceConfig.ZipkinHost) getOrElse "localhost"}:${conf.getInt(ZipkinTraceConfig.ZipkinPort) getOrElse 9410}/api/v1/spans"
      ))
      .build()
    )
    .sampler(conf.getString(ZipkinTraceConfig.ZipkinSampleRate)
      .map(s => Sampler.create(s.toFloat)) getOrElse Sampler.ALWAYS_SAMPLE
    )
    .build()

}
