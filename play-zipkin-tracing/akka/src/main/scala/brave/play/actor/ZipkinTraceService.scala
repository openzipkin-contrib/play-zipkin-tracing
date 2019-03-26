package brave.play.actor

import akka.actor.ActorSystem
import brave.Tracing
import brave.play.{ZipkinTraceConfig, ZipkinTraceServiceLike}
import brave.sampler.Sampler
import zipkin2.reporter.AsyncReporter
import zipkin2.reporter.okhttp3.OkHttpSender

import scala.concurrent.ExecutionContext

/**
 * Class for Zipkin tracing at standalone Akka Actor.
 *
 * @param actorSystem an actor system used for tracing
 * @param serviceName a service name (default is `"unknown"`)
 * @param baseUrl a base url of the Zipkin server (default is `"http://localhost:9411"`)
 * @param sampleRate a sampling rate (default is `None` which means `ALWAYS_SAMPLE`)
 */
class ZipkinTraceService(
  actorSystem: ActorSystem,
  serviceName: String = "unknown",
  baseUrl: String = "http://localhost:9411",
  sampleRate: Option[Float] = None) extends ZipkinTraceServiceLike {

  implicit val executionContext: ExecutionContext = actorSystem.dispatchers.lookup(ZipkinTraceConfig.AkkaName)

  private val sender = OkHttpSender.create(baseUrl + "/api/v2/spans")

  val tracing = Tracing.newBuilder()
    .localServiceName(serviceName)
    .spanReporter(AsyncReporter.create(sender))
    .sampler(sampleRate.map(x => Sampler.create(x)) getOrElse Sampler.ALWAYS_SAMPLE)
    .build()

  def close(): Unit = sender.close()

}