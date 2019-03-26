package brave.play

import brave.Tracing
import brave.internal.HexCodec
import org.scalatest.FunSuite
import zipkin2.Span
import zipkin2.reporter.Reporter

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}


class ZipkinTraceServiceLikeSpec extends FunSuite {

  private def initialTraceData(tracer: ZipkinTraceServiceLike, header: Map[String, String]): TraceData = {
    TraceData(tracer.newSpan[Map[String, String]](header)(getValueFromMap _))
  }

  private def getValueFromMap(map: Map[String, String], key: String): Option[String] = map.get(key)

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

  test("Create span") {
    val tracer = new TestZipkinTraceService()

    // create root span
    val parent = tracer.newSpan[Map[String, String]](Map.empty)(getValueFromMap _)
    assert(parent.context().parentId() == null)

    // create child span
    val child = tracer.newSpan[Map[String, String]](Map(
      "X-B3-TraceId" -> HexCodec.toLowerHex(parent.context().traceId()),
      "X-B3-SpanId"  -> HexCodec.toLowerHex(parent.context().spanId())
    ))(getValueFromMap _)

    assert(child.context().traceId() == parent.context().traceId())
    assert(child.context().parentId() == parent.context().spanId())
    assert(child.context().spanId() != parent.context().spanId())
  }

  test("Receive and send server span") {
    val tracer = new TestZipkinTraceService()

    // create root span
    val span = tracer.newSpan[Map[String, String]](Map.empty)(getValueFromMap _)

    tracer.serverReceived("server-span", span)
    Thread.sleep(500)
    tracer.serverSend(span, "tag" -> "value")

    assert(tracer.reporter.spans.length == 1)

    val reported = tracer.reporter.spans.find(_.name() == "server-span").get
    assert(reported.name() == "server-span")
    assert(reported.duration() >= 500)
    assert(reported.tags().size() == 1)
    assert(reported.tags().get("tag") === "value")
  }

}

class TestZipkinTraceService extends ZipkinTraceServiceLike {
  override implicit val executionContext: ExecutionContext = ExecutionContext.global
  val reporter = new TestReporter()
  override val tracing: Tracing = Tracing.newBuilder().spanReporter(reporter).build()
}

class TestReporter extends Reporter[Span] {
  val spans = new ListBuffer[Span]()
  override def report(span: Span): Unit = spans += span
}