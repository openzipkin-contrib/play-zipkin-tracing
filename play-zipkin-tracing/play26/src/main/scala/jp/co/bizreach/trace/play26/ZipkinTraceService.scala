package jp.co.bizreach.trace.play26

import javax.inject.Inject

import akka.actor.ActorSystem
import brave.Tracing
import brave.http.HttpTracing
import jp.co.bizreach.trace.{ZipkinTraceConfig, ZipkinTraceServiceLike}

import scala.concurrent.ExecutionContext

/**
 * Class for Zipkin tracing at Play2.6.
 *
 * @param tracing     low-level tracing apis and configuration
 * @param httpTracing http-layer tracing apis and configuration
 * @param actorSystem a Play's actor system
 */
class ZipkinTraceService @Inject() (
  val tracing: Tracing,
  override val httpTracing: HttpTracing,
  actorSystem: ActorSystem) extends ZipkinTraceServiceLike {

  implicit val executionContext: ExecutionContext = actorSystem.dispatchers.lookup(ZipkinTraceConfig.AkkaName)
}
