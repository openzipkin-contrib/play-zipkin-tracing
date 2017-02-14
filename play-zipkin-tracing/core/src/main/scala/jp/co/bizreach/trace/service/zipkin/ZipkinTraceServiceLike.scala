package jp.co.bizreach.trace.service.zipkin

import brave.Tracer
import com.github.kristofa.brave.TracerAdapter
import com.twitter.zipkin.gen.Span
import jp.co.bizreach.trace.service.{TraceCassette, TraceServiceLike}
import zipkin.reporter.AsyncReporter

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

/**
  * Created by nishiyama on 2016/12/08.
  */
trait ZipkinTraceServiceLike extends TraceServiceLike {

  type ServerSpan = brave.Span

  implicit val executionContext: ExecutionContext
  val reporter: AsyncReporter[zipkin.Span]
  // brave 4 client
  val clientTracer: Tracer
  // brave 3 client
  val clientBrave = TracerAdapter.newBrave(clientTracer)


  def generateTrace(name: String, cassette: ZipkinTraceCassette): ZipkinTraceCassette = {
    ZipkinTraceCassette(
      span    = TracerAdapter.toSpan(clientTracer.newChild(TracerAdapter.toSpan(clientTracer, cassette.span).context()).name(name).context()),
      sampled = cassette.sampled
    )
  }

  def cassetteToMap(cassette: ZipkinTraceCassette): Map[String, String] = {
    import TraceHttpHeaders._
    toMap(cassette) + (SampledHeaderKey.toString -> (if (cassette.sampled) "1" else "0"))
  }

  def serverReceived(cassette: ZipkinTraceCassette): Future[Option[ServerSpan]] = {
    if (cassette.sampled) {
      Future {
        clientBrave.serverTracer().setStateUnknown(cassette.span.getName)
        clientBrave.serverTracer().setServerReceived()
        Option(TracerAdapter.getServerSpan(clientTracer, clientBrave.serverSpanThreadBinder()))
      }
    } else Future.successful(None)
  }

  def serverSend(cassette: ZipkinTraceCassette, span: ServerSpan, annotations: (String, String)*): Future[ServerSpan] = {
    if (cassette.sampled) {
      Future {
        TracerAdapter.setServerSpan(span.context(), clientBrave.serverSpanThreadBinder())
        annotations.foreach { case (key, value) => clientBrave.serverTracer().submitBinaryAnnotation(key, value) }
        clientBrave.serverTracer().setServerSend()
        span
      }
    } else Future.successful(span)
  }

  override def toMap(cassette: TraceCassette): Map[String, String] = cassette match {
    case zipkin: ZipkinTraceCassette =>
      import SpanHttpHeaders._
      Seq(
        Option(zipkin.span.getTrace_id) .map(_ -> TraceIdHeaderKey),
        Option(zipkin.span.getId)       .map(_ -> SpanIdHeaderKey),
        Option(zipkin.span.getParent_id).map(_ -> ParentIdHeaderKey)
      ).flatten.map {
        case (id: Long, headerKey) => headerKey.toString -> java.lang.Long.toHexString(id)
      }.toMap
    case _ => Map.empty
  }

  override def trace[A](traceName: String, parentCassette: TraceCassette, annotations: Seq[(String, String)])(f: Option[TraceCassette] => A): A = ???

  override def traceFuture[A](traceName: String, parentCassette: TraceCassette, annotations: Seq[(String, String)])(f: Option[TraceCassette] => Future[A]): Future[A] = {
    parentCassette match {
      case cassette: ZipkinTraceCassette if cassette.sampled =>
        sample(traceName, cassette.span, annotations: _*)(o => f(o.map(s => ZipkinTraceCassette(s))).map((_, Seq.empty)))
      case _ =>
        f(Some(parentCassette))
    }
  }

  private[zipkin] def sample[A](name: String, parentSpan: Span, annotations: (String, String)*)(f: Option[Span] => Future[(A, Seq[(String, String)])]): Future[A] = {
    Future {
      // start a new span representing a request
      val childSpan = clientTracer.newChild(TracerAdapter.toSpan(clientTracer, parentSpan).context()).name(name)
      annotations.foreach { case (key, value) => childSpan.tag(key, value) }
      Some(childSpan.start())
    }.recover { case NonFatal(e) => None }
     .flatMap { sentSpan =>
       val result = f(sentSpan.map(x => TracerAdapter.toSpan(x.context())))

       sentSpan.foreach { span =>
         result.onComplete {
           case Failure(t) =>
             span.tag("failed", s"Finished with exception: ${t.getMessage}")
             span.finish()
           case Success((_, endAnnotations)) =>
             endAnnotations.foreach { case (key, value) => span.tag(key, value) }
             span.finish()
         }
       }

       result.map(_._1)
     }
  }


  def timeout: FiniteDuration

}
