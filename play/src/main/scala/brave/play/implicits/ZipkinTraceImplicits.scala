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
package brave.play.implicits

import brave.Span
import brave.play.actor.ActorTraceSupport.ActorTraceData
import brave.play.{TraceData, ZipkinTraceServiceLike}
import play.api.mvc.RequestHeader

trait ZipkinTraceImplicits {

  // for injection
  val tracer: ZipkinTraceServiceLike

  /**
   * Creates a trace data including a span from request headers.
   *
   * @param req the HTTP request header
   * @return the trace data
   */
  implicit def request2trace(implicit req: RequestHeader): TraceData = {
    TraceData(
      span = tracer.toSpan(req.headers)((headers, key) => headers.get(key))
    )
  }

  /**
   * Creates a trace data including a span from request headers for Akka actor.
   *
   * @param req the HTTP request header
   * @return the trace data
   */
  implicit def request2actorTrace(implicit req: RequestHeader): ActorTraceData = {
    val span = tracer.toSpan(req.headers)((headers, key) => headers.get(key))
    val oneWaySpan = tracer.newSpan(Some(span.context())).kind(Span.Kind.CLIENT)
    ActorTraceData(span = oneWaySpan)
  }

}
