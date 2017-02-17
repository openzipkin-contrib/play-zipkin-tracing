package jp.co.bizreach.trace.zipkin

import brave.propagation.Propagation
import brave.{Span, Tracer}
import jp.co.bizreach.trace.{TraceCassette, TraceService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

/**
  * Created by nishiyama on 2016/12/08.
  */
trait ZipkinTraceServiceLike extends TraceService {

  type ServerSpan = Span

  implicit val executionContext: ExecutionContext
  val tracer: Tracer


  def serverReceived[A](traceName: String, span: Span): Future[ServerSpan] = {
    Future {
      span.name(traceName).kind(Span.Kind.SERVER).start()
    }
  }

  def serverSend(span: ServerSpan, tags: (String, String)*): Future[ServerSpan] = {
    Future {
      tags.foreach { case (key, value) => span.tag(key, value) }
      span.finish()
      span
    }
  }

  // Request Headers => Span
  def toSpan[A](headers: A)(getHeader: (A, String) => Option[String]): Span = {
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

  def toMap(cassette: TraceCassette): Map[String, String] = cassette match {
    case zipkin: ZipkinTraceCassette => toMap(zipkin.span)
    case _ => Map.empty
  }

  override def trace[A](traceName: String, tags: (String, String)*)(f: TraceCassette => A)(implicit parentCassette: TraceCassette): A = ???

  override def traceFuture[A](traceName: String, tags: (String, String)*)(f: TraceCassette => Future[A])(implicit parentCassette: TraceCassette): Future[A] = {
    parentCassette match {
      case cassette: ZipkinTraceCassette =>
        sample(traceName, cassette.span, tags: _*)(s => f(ZipkinTraceCassette(s)))
      case _ =>
        f(parentCassette)
    }
  }

  private[zipkin] def sample[A](name: String, parent: Span, tags: (String, String)*)(f: Span => Future[A]): Future[A] = {
    Future {
      // start a new span representing a request
      val childSpan = tracer.newChild(parent.context()).name(name).kind(Span.Kind.CLIENT)
      tags.foreach { case (key, value) => childSpan.tag(key, value) }
      childSpan.start()
    }.flatMap { span =>
      val result = f(span)
      result.onComplete {
        case Failure(t) =>
          span.tag("failed", s"Finished with exception: ${t.getMessage}")
          span.finish()
        case _ =>
          span.finish()
      }
      result
    }
  }

}
