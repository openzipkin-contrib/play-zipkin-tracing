package brave.play

import javax.inject.Inject

import akka.actor.ActorSystem
import brave.Tracing

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
