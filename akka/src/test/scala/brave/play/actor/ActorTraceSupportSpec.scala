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
package brave.play.actor

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import brave.Tracing
import brave.play.ZipkinTraceServiceLike
import brave.play.actor.ActorTraceSupport._
import brave.test.TestSpanHandler
import org.scalatest.AsyncFlatSpec

import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`
import scala.concurrent.ExecutionContext


class ActorTraceSupportSpec extends AsyncFlatSpec {

  it should "ask pattern" in {
    val system = ActorSystem("mySystem")
    implicit val tracer = new TestZipkinTraceService
    implicit val timeout = Timeout(5, TimeUnit.SECONDS)

    val actor = system.actorOf(Props(classOf[HelloWorldActor], tracer), "test-actor")

    TraceableActorRef(actor) ? HelloWorldMessage("Test", ActorTraceData()) map { result =>
      assert(result == "Received data: Test")
    }

    TimeUnit.SECONDS.sleep(3)
    tracer.tracing.close()
    system.terminate()

    assert(tracer.spanHandler.size == 2)
    val parent = tracer.spanHandler.find(_.name == "? - test-actor").get
    val child  = tracer.spanHandler.find(_.name == "test-actor").get
    assert(parent.id == child.parentId)
    assert(parent.id != child.id)
  }

}

class HelloWorldActor(val tracer: ZipkinTraceServiceLike) extends TraceableActor {
  def receive = {
    case m: HelloWorldMessage =>
      sender() ! s"Received data: ${m.message}"
  }
}

case class HelloWorldMessage(message: String, traceData: ActorTraceData) extends TraceMessage

class TestZipkinTraceService extends ZipkinTraceServiceLike {
  override implicit val executionContext: ExecutionContext = ExecutionContext.global
  val spanHandler = new TestSpanHandler()
  override val tracing: Tracing = Tracing.newBuilder().addSpanHandler(spanHandler).build()
}
