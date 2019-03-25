package zipkin.play.actor

import akka.AroundReceiveOverrideHack
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import brave.Span
import zipkin.play.ZipkinTraceServiceLike

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
   */
  trait TraceMessage {
    val traceData: ActorTraceData
  }

  /**
   * Mix-in this trait to message class instead of [[TraceMessage]] if the target actor is a remote actor.
   *
   * {{{
   * case class HelloActorMessage(message: String)(implicit val traceData: RemoteActorTraceData)
   *   extends RemoteTraceMessage
   * }}}
   */
  trait RemoteTraceMessage {
    val traceData: RemoteActorTraceData
  }

  case class ActorTraceData(span: Span)

  object ActorTraceData {
    def apply()(implicit tracer: ZipkinTraceServiceLike): ActorTraceData = {
      ActorTraceData(tracer.newSpan(None).kind(Span.Kind.CLIENT))
    }
  }

  type RemoteSpan = java.util.HashMap[String, String]

  case class RemoteActorTraceData(span: RemoteSpan)

  object RemoteActorTraceData {
    def apply()(implicit tracer: ZipkinTraceServiceLike): RemoteActorTraceData = {
      RemoteActorTraceData(toRemoteSpan(tracer.newSpan(None).kind(Span.Kind.CLIENT), tracer))
    }
  }

  private def toRemoteSpan(span: Span, tracer: ZipkinTraceServiceLike): RemoteSpan = {
    val data = new RemoteSpan()
    tracer.tracing.propagation().injector(
      (carrier: RemoteSpan, key: String, value: String) => carrier.put(key, value)
    ).inject(span.context(), data)
    data
  }

  private def fromRemoteSpan(data: RemoteSpan, tracer: ZipkinTraceServiceLike): Span = {
    val contextOrFlags = tracer.tracing.propagation().extractor(
      (carrier: RemoteSpan, key: String) => carrier.get(key)
    ).extract(data)
    tracer.tracing.tracer.joinSpan(contextOrFlags.context())
  }

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
    implicit def remoteTraceData: RemoteActorTraceData = RemoteActorTraceData(toRemoteSpan(traceData.span, tracer))

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
        case m: RemoteTraceMessage =>
          val serverSpan = tracer.newSpan(Some(fromRemoteSpan(m.traceData.span, tracer).context)).kind(Span.Kind.SERVER)
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
    def ! (message: Any): Unit = {
      actorRef ! message
      message match {
        case m: TraceMessage =>
          m.traceData.span.name("! - " + actorRef.path.name).start().flush()
        case m: RemoteTraceMessage =>
          val span = fromRemoteSpan(m.traceData.span, tracer)
          span.name("! - " + actorRef.path.name).start().flush()
        case _ =>
      }
    }

    def ? (message: Any)(implicit timeout: Timeout): Future[Any] = {
      val f = actorRef ? message
      message match {
        case m: TraceMessage =>
          m.traceData.span.name("? - " + actorRef.path.name).start()
          f.onComplete {
            case Failure(t) =>
              m.traceData.span.tag("failed", s"Finished with exception: ${t.getMessage}")
              m.traceData.span.finish()
            case _ =>
              m.traceData.span.finish()
          }(tracer.executionContext)

        case m: RemoteTraceMessage =>
          val span = fromRemoteSpan(m.traceData.span, tracer)
          span.name("? - " + actorRef.path.name).start()
          f.onComplete {
            case Failure(t) =>
              span.tag("failed", s"Finished with exception: ${t.getMessage}")
              span.finish()
            case _ =>
              span.finish()
          }(tracer.executionContext)
        case _ =>
      }
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
