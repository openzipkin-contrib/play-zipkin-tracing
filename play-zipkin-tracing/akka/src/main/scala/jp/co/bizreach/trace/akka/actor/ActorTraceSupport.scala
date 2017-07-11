package jp.co.bizreach.trace.akka.actor

import akka.AroundReceiveOverrideHack
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import brave.Span
import jp.co.bizreach.trace.ZipkinTraceServiceLike

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object ActorTraceSupport {

  /**
   * Mix-in this trait to message class for traced actor.
   * Typically, traceData property would be declared as `implicit val` as follows:
   *
   * {{{
   * case class HelloActorMessage(message: String)(implicit val traceData: ActorTraceData)
   *   extends TraceMessage
   * }}}
   *
   * TODO TraceData is available for only local actors. We have to invent another solution for remote actors.
   */
  trait TraceMessage {
    val traceData: ActorTraceData
  }

  case class ActorTraceData(span: Span)

  /**
   * Traceable actor have to extend this trait.
   *
   * {{{
   * class HelloActor @Inject()(val tracer: ZipkinTraceServiceLike) extends TraceableActor {
   *   def receive = {
   *     case m: HelloActorMessage => {
   *       println(m.message)
   *     }
   *   }
   * }
   * }}}
   *
   * You can call this actor by injecting its `ActorRef` by Play's dependency injection.
   */
  trait TraceableActor extends AroundReceiveOverrideHack {

    val tracer: ZipkinTraceServiceLike

    implicit var traceData: ActorTraceData = null

    override protected def aroundReceiveMessage(receive: Receive, msg: Any): Unit = {
      msg match {
        case m: TraceMessage =>
          val serverSpan = tracer.newSpan(Some(m.traceData.span.context)).kind(Span.Kind.SERVER)
          tracer.serverReceived(self.path.name, serverSpan)

          val clientSpan = tracer.newSpan(Some(serverSpan.context)).kind(Span.Kind.CLIENT)
          this.traceData = ActorTraceData(clientSpan)

          Try(super.aroundReceiveMessage(receive, msg)) match {
            case Failure(t) =>
              serverSpan.tag("failed", s"Finished with exception: ${t.getMessage}")
              tracer.serverSend(serverSpan)
              throw t
            case Success(_) =>
              tracer.serverSend(serverSpan)
          }
        case _ =>
          val clientSpan = tracer.newSpan(None).kind(Span.Kind.CLIENT)
          this.traceData = ActorTraceData(clientSpan)
          super.aroundReceiveMessage(receive, msg)
      }
    }
  }

  class TraceableActorRef(actorRef: ActorRef, tracer: ZipkinTraceServiceLike){
    def ! [T <: TraceMessage](message: T): Unit = {
      actorRef ! message
      message.traceData.span.name("! - " + actorRef.path.name).start().flush()
    }

    def ? [T <: TraceMessage](message: T)(implicit timeout: Timeout): Future[Any] = {
      val f = actorRef ? message
      message.traceData.span.name("? - " + actorRef.path.name).start()
      f.onComplete {
        case Failure(t) =>
          message.traceData.span.tag("failed", s"Finished with exception: ${t.getMessage}")
          message.traceData.span.finish()
        case _ =>
          message.traceData.span.finish()
      }(tracer.executionContext)
      f
    }
  }

  /**
   * Decorate `ActorRef` of [[TraceableActor]].
   *
   * {{{
   * // tell
   * TraceableActorRef(actorRef) ! message
   *
   * // ask
   * val f: Future[Any] = TraceableActorRef(actorRef) ? message
   * }}}
   */
  object TraceableActorRef {
    def apply(actorRef: ActorRef)(implicit tracer: ZipkinTraceServiceLike): TraceableActorRef = {
      new TraceableActorRef(actorRef, tracer)
    }
  }

}
