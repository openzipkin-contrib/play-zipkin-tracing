package jp.co.bizreach.trace.service.zipkin

import com.beachape.zipkin.services.ZipkinServiceLike
import com.twitter.zipkin.gen.Span
import jp.co.bizreach.trace.service.{TraceCassette, TraceServiceLike}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Random, Success, Try}

/**
  * Created by nishiyama on 2016/12/08.
  */
trait ZipkinTraceServiceLike extends TraceServiceLike {

  val service: ZipkinServiceLike
  val sampleRate: Double

  def generateTrace(name: String, cassette: ZipkinTraceCassette): ZipkinTraceCassette = {
    ZipkinTraceCassette(service.generateSpan(name, cassette.span), cassette.sampled)
  }

  def cassetteToMap(cassette: ZipkinTraceCassette): Map[String, String] =  {
    import TraceHttpHeaders._

    service.spanToIdsMap(cassette.span) + (SampledHeaderKey.toString -> (if (cassette.sampled) "1" else "0"))
  }

  def isSampled: Boolean = {
    def rate = sampleRate match {
      case s if s > 1.0 => 1.0
      case s if s < 0.0 => 0.0
      case s => s
    }
    rate >= Random.nextDouble()
  }

  def serverReceived(cassette: ZipkinTraceCassette, annotations: (String, String)*): Future[Option[service.ServerSpan]] = {
   if (cassette.sampled) {
     service.serverReceived(cassette.span, annotations: _*)
   } else Future.successful(None)
 }


  def serverSent(cassette: ZipkinTraceCassette, serverSpan: service.ServerSpan, annotations: (String, String)*): Future[service.ServerSpan] = {
    if (cassette.sampled) {
      service.serverSent(serverSpan, annotations: _*)
    } else Future.successful(serverSpan)
  }

  override def toMap(cassette: TraceCassette): Map[String, String] = {
    cassette match {
      case zipkinCassette: ZipkinTraceCassette =>
        service.spanToIdsMap(zipkinCassette.span)
      case _ => Map.empty
    }
  }

  override def traceFuture[A](traceName: String, parentCassette: TraceCassette, annotations: Seq[(String, String)])(f: Option[TraceCassette] => Future[A]): Future[A] = {
    import service.eCtx // Because tracing-related tasks should use the same ExecutionContext

    parentCassette match {
      case zipkinParentCassette: ZipkinTraceCassette if zipkinParentCassette.sampled =>
        endAnnotations(traceName, zipkinParentCassette.span, annotations: _*)(o => f(o.map(s => ZipkinTraceCassette(s))).map((_, Seq.empty)))
      case _ =>
        f(Some(parentCassette))
    }
  }

  def endAnnotations[A](traceName: String, parentSpan: Span, annotations: (String, String)*)(f: Option[Span] => Future[(A, Seq[(String, String)])]): Future[A] = {
    import service.eCtx // Because tracing-related tasks should use the same ExecutionContext

    Try(service.generateSpan(traceName, parentSpan)).toOption.fold {
      // TODO: デバッグログ
      f(None).map(_._1)
    }{ childSpan =>
      val fMaybeSentCustomSpan = service.clientSent(childSpan, annotations: _*).recover { case NonFatal(e) => None }
      val fResult = for {
        maybeSentCustomSpan <- fMaybeSentCustomSpan
        maybeNormalSentSpan = maybeSentCustomSpan.map(c => service.clientSpanToSpan(c).deepCopy())
        result <- f(maybeNormalSentSpan)
      } yield result
      fMaybeSentCustomSpan foreach { maybeSentCustomSpan =>
        maybeSentCustomSpan foreach { sentCustomSpan =>
          fResult.onComplete {
            case Success((_, endAnnotations)) => service.clientReceived(sentCustomSpan, endAnnotations: _*)
            case Failure(e) => service.clientReceived(sentCustomSpan, "failed" -> s"Finished with exception${Option(e.getMessage).fold("") { m => s": $m" }}")
          }
        }
      }
      fResult.map(_._1)
    }
  }

  override def trace[A](traceName: String, parentCassete: TraceCassette, annotations: Seq[(String, String)])(f: Option[TraceCassette] => A): A = {
    parentCassete match {
      case zipkinParentCassette: ZipkinTraceCassette if zipkinParentCassette.sampled =>
        simple(traceName, zipkinParentCassette.span, annotations: _*)(a => (f(a), Seq.empty))
      case _ =>
        f(Some(parentCassete))
    }
  }

  protected def simple[A](traceName: String, parentSpan: Span, annotations: (String, String)*)(f: Option[TraceCassette] => (A, Seq[(String, String)])): A = {
    import service.eCtx

    Try(service.generateSpan(traceName, parentSpan)).toOption.fold {
      f(None)._1
    } { childSpan =>
      val fMaybeSentCustomSpan = service.clientSent(childSpan, annotations: _*).recover { case NonFatal(e) => None }
      //Tries to wait for a certain amount of time for the ZipkinService to provide a span to to use
      val maybeSentProvided = Try(Await.result(fMaybeSentCustomSpan, timeout)).getOrElse(None)
      val tryResult = Try(f(maybeSentProvided.map(s => ZipkinTraceCassette(service.clientSpanToSpan(s).deepCopy()))))
      tryResult match {
        case Success(result) => {
          fMaybeSentCustomSpan.foreach { maybeActuallySent =>
            maybeActuallySent orElse maybeSentProvided foreach { span =>
              service.clientReceived(span, result._2: _*)
            }
          }
          result._1
        }
        case Failure(e) => {
          fMaybeSentCustomSpan.foreach { maybeActuallySent =>
            maybeActuallySent orElse maybeSentProvided foreach { span =>
              service.clientReceived(span, "failed" -> s"Finished with exception${Option(e.getMessage).fold("") { m => s": $m" }}")
            }
          }
          throw e
        }
      }
    }
  }

  def timeout: FiniteDuration

}
