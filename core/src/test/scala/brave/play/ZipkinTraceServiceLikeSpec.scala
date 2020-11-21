/**
 * Copyright 2017-2020 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package brave.play

import brave.Tracing
import brave.test.TestSpanHandler
import org.scalatest.FunSuite

import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`
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

    assert(tracer.spanHandler.size == 2)

    val parent = tracer.spanHandler.find(_.name == "trace-1").get
    val child  = tracer.spanHandler.find(_.name == "trace-2").get

    assert(parent.id == child.parentId)
    assert(parent.id != child.id)
    assert(parent.finishTimestamp > child.finishTimestamp)
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

    assert(tracer.spanHandler.size == 2)
    val parent = tracer.spanHandler.find(_.name == "trace-future").get
    val child  = tracer.spanHandler.find(_.name == "trace-sync").get

    assert(parent.finishTimestamp - parent.startTimestamp >= 500)
    assert(parent.id == child.parentId)
    assert(parent.id != child.id)
    assert(parent.finishTimestamp > child.finishTimestamp)
  }

  test("Create span") {
    val tracer = new TestZipkinTraceService()

    // create root span
    val parent = tracer.newSpan[Map[String, String]](Map.empty)(getValueFromMap _)
    assert(parent.context().parentId() == null)

    // create child span
    val child = tracer.newSpan[Map[String, String]](Map(
      "X-B3-TraceId" -> parent.context().traceIdString(),
      "X-B3-SpanId"  -> parent.context().spanIdString()
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

    assert(tracer.spanHandler.size == 1)

    val reported = tracer.spanHandler.find(_.name == "server-span").get
    assert(reported.name == "server-span")
    assert(reported.finishTimestamp - reported.startTimestamp >= 500)
    assert(reported.tags.size == 1)
    assert(reported.tags.get("tag") === "value")
  }

}

class TestZipkinTraceService extends ZipkinTraceServiceLike {
  override implicit val executionContext: ExecutionContext = ExecutionContext.global
  val spanHandler = new TestSpanHandler()
  override val tracing: Tracing = Tracing.newBuilder().addSpanHandler(spanHandler).build()
}
