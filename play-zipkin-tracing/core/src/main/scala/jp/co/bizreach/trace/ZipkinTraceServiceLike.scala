package jp.co.bizreach.trace

import brave.propagation.{Propagation, TraceContext}
import brave.{Span, Tracer, Tracing}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * Basic trait for Zipkin tracing at Play.
 *
 * You need a Tracing and an ExecutionContext used by a tracer report data.
 * Here's an example setup that sends trace data to Zipkin over http.
 * {{{
 * class ZipkinTraceService @Inject() (
 *   actorSystem: ActorSystem) extends ZipkinTraceServiceLike {
 *
 *   // the execution context provided by and used for tracing purposes
 *   implicit val executionContext: ExecutionContext = actorSystem.dispatchers.lookup("zipkin-trace-context")
 *
 *   // configure a reporter, now create a tracing component
 *   val tracing = Tracing.newBuilder()
 *     .localServiceName("example")
 *     .reporter(AsyncReporter
 *       .builder(OkHttpSender.create("http://localhost:9411/api/v1/spans"))
 *       .build()
 *     )
 *     .sampler(Sampler.create(0.1F))
 *     .build()
 * }
 * }}}
 *
 */
trait ZipkinTraceServiceLike {

  type ServerSpan = Span

  // used by a tracer report data to Zipkin
  implicit val executionContext: ExecutionContext
  val tracing: Tracing
  val mapInjector = tracing.propagation().injector(ZipkinTraceServiceLike.mapSetter)

  private def tracer: Tracer = tracing.tracer


  /**
   * Creates a new client span within an existing trace and reports the span complete.
   * When an `Action` that returns a result, calling this.
   *
   * @param traceName the string name for this trace
   * @param tags tags to add to the trace
   * @param f the function to trace
   * @param parentData an existing parent trace data
   * @tparam A return type of the function to trace
   * @return result of invoking the function
   */
  def trace[A](traceName: String, tags: (String, String)*)(f: TraceData => A)(implicit parentData: TraceData): A = {
    val childSpan = tracer.newChild(parentData.span.context()).name(traceName).kind(Span.Kind.CLIENT)
    tags.foreach { case (key, value) => childSpan.tag(key, value) }
    childSpan.start()

    Try(f(TraceData(childSpan))) match {
      case Failure(t) =>
        childSpan.tag("failed", s"Finished with exception: ${t.getMessage}")
        childSpan.finish()
        throw t
      case Success(result) =>
        childSpan.finish()
        result
    }
  }

  /**
   * Creates a new client span within an existing trace and reports the span complete.
   * When an `Action` that returns a future of a result, calling this.
   *
   * @param traceName the string name for this trace
   * @param tags tags to add to the trace
   * @param f the function to trace
   * @param parentData an existing parent trace data
   * @tparam A return type of the function to trace
   * @return result of invoking the function
   */
  def traceFuture[A](traceName: String, tags: (String, String)*)(f: TraceData => Future[A])(implicit parentData: TraceData): Future[A] = {
    val childSpan = tracer.newChild(parentData.span.context()).name(traceName).kind(Span.Kind.CLIENT)
    tags.foreach { case (key, value) => childSpan.tag(key, value) }
    childSpan.start()

    val result = f(TraceData(childSpan))

    result.onComplete {
      case Failure(t) =>
        childSpan.tag("failed", s"Finished with exception: ${t.getMessage}")
        childSpan.finish()
      case _ =>
        childSpan.finish()
    }

    result
  }

  /**
   * Starts the server span. When a server received event has occurred, calling this.
   *
   * @param spanName the string name for this span
   * @param span the span to start
   * @return the server span that will later signal to the Zipkin
   */
  private[trace] def serverReceived(spanName: String, span: Span): ServerSpan = {
    span.name(spanName).kind(Span.Kind.SERVER).start()
  }

  /**
   * Reports the server span complete. When a server sent event has occurred, calling this.
   *
   * @param span the server span to report
   * @param tags tags to add to the span
   * @return the server span itself
   */
  private[trace] def serverSend(span: ServerSpan, tags: (String, String)*): ServerSpan = {
    tags.foreach { case (key, value) => span.tag(key, value) }
    span.finish()
    span
  }

  /**
   * Creates a span from request headers. If there is no existing trace, creates a new trace.
   * Otherwise creates a new span within an existing trace.
   *
   * @param headers the HTTP headers
   * @param getHeader optionally returns the first header value associated with a key
   * @tparam A the HTTP headers type
   * @return a new span created from request headers
   */
  private[trace] def newSpan[A](headers: A)(getHeader: (A, String) => Option[String]): Span = {
    val contextOrFlags = tracing.propagation().extractor(
      (carrier: A, key: String) => getHeader(carrier, key).orNull
    ).extract(headers)

    Option(contextOrFlags.context())
      .map(tracer.newChild)
      .getOrElse(tracer.newTrace(contextOrFlags.samplingFlags()))
  }

  /**
   * Creates a span from a parent context.
   * If the parent span is None, creates a new trace.
   *
   * @param parent the parent context
   * @return a new span created from the parent context
   */
  private[trace] def newSpan(parent: Option[TraceContext]): Span = {
    parent match {
      case Some(x) => tracer.newChild(x)
      case None    => tracer.newTrace()
    }
  }

  /**
   * Transform request headers to the span. This method is reusing
   * the ids extracted from an incoming request.
   *
   * @param headers the HTTP headers
   * @param getHeader optionally returns the first header value associated with a key
   * @tparam A the HTTP headers type
   * @return a span extracted from request headers
   */
  private[trace] def toSpan[A](headers: A)(getHeader: (A, String) => Option[String]): Span = {
    val contextOrFlags = tracing.propagation().extractor(
      (carrier: A, key: String) => getHeader(carrier, key).orNull
    ).extract(headers)

    tracer.joinSpan(contextOrFlags.context())
  }

  /**
   * Transform the span to a request header's Map.
   *
   * @param span the span
   * @return a Map transformed the span
   */
  private[trace] def toMap(span: Span): Map[String, String] = {
    val data = collection.mutable.Map[String, String]()

    mapInjector.inject(span.context(), data)

    data.toMap
  }

  /**
   * Transform the trace data to a request header's Map.
   *
   * @param traceData the trace data
   * @return a Map transformed the trace data
   */
  private[trace] def toMap(traceData: TraceData): Map[String, String] = {
    toMap(traceData.span)
  }

}
object ZipkinTraceServiceLike {
  val mapSetter: Propagation.Setter[collection.mutable.Map[String, String], String] =
    (carrier, key, value) => carrier += key -> value
}
