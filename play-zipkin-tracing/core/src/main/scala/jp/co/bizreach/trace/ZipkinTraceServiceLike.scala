package jp.co.bizreach.trace

import brave.propagation.Propagation
import brave.{Span, Tracer}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try, Failure}

/**
  * Created by nishiyama on 2016/12/08.
  */
trait ZipkinTraceServiceLike {

  type ServerSpan = Span

  implicit val executionContext: ExecutionContext
  val tracer: Tracer


  def serverReceived[A](spanName: String, span: Span): ServerSpan = {
    span.name(spanName).kind(Span.Kind.SERVER).start()
  }

  def serverSend(span: ServerSpan, tags: (String, String)*): ServerSpan = {
    Future {
      tags.foreach { case (key, value) => span.tag(key, value) }
      span.finish()
    }
    span
  }

  // Request Headers => Span
  def newSpan[A](headers: A)(getHeader: (A, String) => Option[String]): Span = {
    val contextOrFlags = Propagation.B3_STRING.extractor(new Propagation.Getter[A, String] {
      def get(carrier: A, key: String): String = getHeader(carrier, key).orNull
    }).extract(headers)

    Option(contextOrFlags.context())
      .map(tracer.newChild)
      .getOrElse(tracer.newTrace(contextOrFlags.samplingFlags()))
  }

  // Span => Request Headers
  def toMap(span: Span): Map[String, String] = {
    val data = collection.mutable.Map[String, String]()

    Propagation.B3_STRING.injector(new Propagation.Setter[collection.mutable.Map[String, String], String] {
      def put(carrier: collection.mutable.Map[String, String], key: String, value: String): Unit = carrier += key -> value
    }).inject(span.context(), data)

    data.toMap
  }

  def toMap(traceData: TraceData): Map[String, String] = {
    toMap(traceData.span)
  }

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
