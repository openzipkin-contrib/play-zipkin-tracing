package jp.co.bizreach.trace.play

import javax.inject.Inject

import akka.actor.ActorSystem
import brave.Tracing
import jp.co.bizreach.trace.{ZipkinTraceConfig, ZipkinTraceServiceLike}

import scala.concurrent.ExecutionContext

/**
 * Class for Zipkin tracing at Play2.7.
 *
 * @param tracing a Play's configuration
 * @param actorSystem a Play's actor system
 */
class ZipkinTraceService @Inject() (
  val tracing: Tracing,
  actorSystem: ActorSystem) extends ZipkinTraceServiceLike {

  implicit val executionContext: ExecutionContext = actorSystem.dispatchers.lookup(ZipkinTraceConfig.AkkaName)
}
