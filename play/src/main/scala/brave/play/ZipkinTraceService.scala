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

import akka.actor.ActorSystem
import brave.Tracing
import javax.inject.Inject

import scala.concurrent.ExecutionContext

/**
 * Class for Zipkin tracing at Play2.7.
 *
 * @param tracing a Play's configuration
 * @param actorSystem a Play's actor system
 */
class ZipkinTraceService @Inject() (
  val tracing: Tracing,
  actorSystem: ActorSystem) extends ZipkinTraceServiceLike {

  implicit val executionContext: ExecutionContext = actorSystem.dispatchers.lookup(ZipkinTraceConfig.AkkaName)
}
