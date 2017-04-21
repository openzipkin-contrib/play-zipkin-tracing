package jp.co.bizreach.trace

import brave.Tracer
import org.scalatest.FunSuite
import zipkin.Span
import zipkin.reporter.Reporter

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}


class ZipkinTraceServiceLikeSpec extends FunSuite {

  private def initialTraceData(tracer: ZipkinTraceServiceLike, header: Map[String, String]): TraceData = {
    TraceData(tracer.newSpan[Map[String, String]](header)((headers: Map[String, String], key: String) => headers.get(key)))
  }

  test("Nested local synchronous tracing"){
    val tracer = new TestZipkinTraceService()
    implicit val traceData = initialTraceData(tracer, Map.empty)

    tracer.trace("trace-1"){ implicit traceData =>
      tracer.trace("trace-2"){ implicit traceData =>
        println("Hello World!")
      }
    }

    assert(tracer.reporter.spans.length == 2)

    val parent = tracer.reporter.spans.find(_.name == "trace-1").get
    val child  = tracer.reporter.spans.find(_.name == "trace-2").get

    assert(parent.id == child.parentId)
    assert(parent.id != child.id)
    assert(parent.duration > child.duration)
  }

  test("Future and a nested local synchronous process tracing") {
    import scala.concurrent.ExecutionContext.Implicits.global

    val tracer = new TestZipkinTraceService()
    implicit val traceData = initialTraceData(tracer, Map.empty)

    val f = tracer.traceFuture("trace-future") { implicit traceData =>
      Future {
        tracer.trace("trace-sync") { _ =>
          Thread.sleep(500)
        }
      }
    }

    Await.result(f, Duration.Inf)
    Thread.sleep(100) // wait for callback completion

    assert(tracer.reporter.spans.length == 2)
    val parent = tracer.reporter.spans.find(_.name == "trace-future").get
    val child  = tracer.reporter.spans.find(_.name == "trace-sync").get

    assert(parent.duration >= 500)
    assert(parent.id == child.parentId)
    assert(parent.id != child.id)
    assert(parent.duration > child.duration)
  }


}

class TestZipkinTraceService extends ZipkinTraceServiceLike {
  override implicit val executionContext: ExecutionContext = ExecutionContext.global
  val reporter = new TestReporter()
  override val tracer: Tracer = Tracer.newBuilder().reporter(reporter).build()
}

class TestReporter extends Reporter[Span] {
  val spans = new ListBuffer[Span]()
  override def report(span: Span): Unit = spans += span
}