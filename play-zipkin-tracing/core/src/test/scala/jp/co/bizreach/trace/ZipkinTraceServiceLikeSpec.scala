package jp.co.bizreach.trace

import brave.Tracer
import org.scalatest.FunSuite
import zipkin.Span
import zipkin.reporter.Reporter

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext


class ZipkinTraceServiceLikeSpec extends FunSuite {

  private def initialTraceData(tracer: ZipkinTraceServiceLike, header: Map[String, String]): TraceData = {
    TraceData(tracer.newSpan[Map[String, String]](header)((headers: Map[String, String], key: String) => headers.get(key)))
  }

  test("Nested client tracing"){
    val tracer = new TestZipkinTraceService()
    implicit val traceData = initialTraceData(tracer, Map.empty)

    tracer.trace("trace-1"){ implicit traceData =>
      tracer.trace("trace-2"){ implicit traceData =>
        println("Hello World!")
      }
    }

    Thread.sleep(500)
    assert(tracer.reporter.spans.length == 2)

    val parent = tracer.reporter.spans.find(_.name == "trace-1").get
    val child  = tracer.reporter.spans.find(_.name == "trace-2").get

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