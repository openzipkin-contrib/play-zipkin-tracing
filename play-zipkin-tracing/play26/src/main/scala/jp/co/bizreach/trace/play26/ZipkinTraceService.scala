package jp.co.bizreach.trace.play26

import javax.inject.Inject

import akka.actor.ActorSystem
import brave.Tracing
import brave.sampler.Sampler
import jp.co.bizreach.trace.{ZipkinTraceConfig, ZipkinTraceServiceLike}
import play.api.Configuration
import zipkin.reporter.AsyncReporter
import zipkin.reporter.okhttp3.OkHttpSender

import scala.concurrent.ExecutionContext

/**
 * Class for Zipkin tracing at Play2.6.
 *
 * @param conf a Play's configuration
 * @param actorSystem a Play's actor system
 */
class ZipkinTraceService @Inject() (
  conf: Configuration,
  actorSystem: ActorSystem) extends ZipkinTraceServiceLike {

  implicit val executionContext: ExecutionContext = actorSystem.dispatchers.lookup(ZipkinTraceConfig.AkkaName)

  val tracing = Tracing.newBuilder()
    .localServiceName(conf.getOptional[String](ZipkinTraceConfig.ServiceName) getOrElse "unknown")
    .reporter(AsyncReporter
      .builder(OkHttpSender.create(
        (conf.getOptional[String](ZipkinTraceConfig.ZipkinBaseUrl) getOrElse "http://localhost:9411") + "/api/v1/spans"
      ))
      .build()
    )
    .sampler(conf.getOptional[String](ZipkinTraceConfig.ZipkinSampleRate)
      .map(s => Sampler.create(s.toFloat)) getOrElse Sampler.ALWAYS_SAMPLE
    )
    .build()

}
