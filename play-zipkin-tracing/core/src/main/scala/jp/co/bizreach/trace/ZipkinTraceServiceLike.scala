package jp.co.bizreach.trace

import brave.propagation.Propagation
import brave.{Span, Tracer}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try, Failure}

/**
 * Basic trait for Zipkin tracing at Play.
 *
 * You need a Tracer and an ExecutionContext used by a tracer report data.
 * Here's an example setup that sends trace data to Zipkin over http.
 * {{{
 * class ZipkinTraceService @Inject() (
 *   actorSystem: ActorSystem) extends ZipkinTraceServiceLike {
 *
 *   // the execution context provided by and used for tracing purposes
 *   implicit val executionContext: ExecutionContext = actorSystem.dispatchers.lookup("zipkin-trace-context")
 *
 *   // configure a reporter, now create a tracer
 *   val tracer = Tracer.newBuilder()
 *     .localServiceName("example")
 *     .reporter(AsyncReporter
 *       .builder(OkHttpSender.create("http://localhost:9410/api/v1/spans"))
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
  val tracer: Tracer

  /**
   * Starts the server span. When a server received event has occurred, calling this.
   *
   * @param spanName the string name for this span
   * @param span the span to start
   * @return the server span that will later signal to the Zipkin
   */
  def serverReceived(spanName: String, span: Span): ServerSpan = {
    span.name(spanName).kind(Span.Kind.SERVER).start()
  }

  /**
   * Reports the server span complete. When a server sent event has occurred, calling this.
   *
   * @param span the server span to report
   * @param tags tags to add to the span
   * @return the server span itself
   */
  def serverSend(span: ServerSpan, tags: (String, String)*): ServerSpan = {
    Future {
      tags.foreach { case (key, value) => span.tag(key, value) }
      span.finish()
    }
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
  def newSpan[A](headers: A)(getHeader: (A, String) => Option[String]): Span = {
    val contextOrFlags = Propagation.B3_STRING.extractor(new Propagation.Getter[A, String] {
      def get(carrier: A, key: String): String = getHeader(carrier, key).orNull
    }).extract(headers)

    Option(contextOrFlags.context())
      .map(tracer.newChild)
      .getOrElse(tracer.newTrace(contextOrFlags.samplingFlags()))
  }

  /**
   * Transform the span to a request header's Map.
   *
   * @param span the span
   * @return a Map transformed the span
   */
  def toMap(span: Span): Map[String, String] = {
    val data = collection.mutable.Map[String, String]()

    Propagation.B3_STRING.injector(new Propagation.Setter[collection.mutable.Map[String, String], String] {
      def put(carrier: collection.mutable.Map[String, String], key: String, value: String): Unit = carrier += key -> value
    }).inject(span.context(), data)

    data.toMap
  }

  /**
   * Transform the trace data to a request header's Map.
   *
   * @param traceData the trace data
   * @return a Map transformed the trace data
   */
  def toMap(traceData: TraceData): Map[String, String] = {
    toMap(traceData.span)
  }

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
  def trace[A](traceName: String, tags: (String, String)*)(f: => A)(implicit parentData: TraceData): A = {
    val childSpan = tracer.newChild(parentData.span.context()).name(traceName).kind(Span.Kind.CLIENT)
    tags.foreach { case (key, value) => childSpan.tag(key, value) }
    childSpan.start()

    Try(f) match {
      case Failure(t) =>
        Future {
          childSpan.tag("failed", s"Finished with exception: ${t.getMessage}")
          childSpan.finish()
        }
        throw t
      case Success(result) =>
        Future {
          childSpan.finish()
        }
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
  def traceFuture[A](traceName: String, tags: (String, String)*)(f: => Future[A])(implicit parentData: TraceData): Future[A] = {
    val childSpan = tracer.newChild(parentData.span.context()).name(traceName).kind(Span.Kind.CLIENT)
    tags.foreach { case (key, value) => childSpan.tag(key, value) }
    childSpan.start()

    val result = f

    result.onComplete {
      case Failure(t) =>
        childSpan.tag("failed", s"Finished with exception: ${t.getMessage}")
        childSpan.finish()
      case _ =>
        childSpan.finish()
    }

    result
  }

}
