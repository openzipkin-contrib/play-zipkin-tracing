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

import akka.actor.ActorSystem
import brave.Tracing
import brave.play.{ZipkinTraceConfig, ZipkinTraceServiceLike}
import brave.sampler.Sampler
import zipkin2.reporter.AsyncReporter
import zipkin2.reporter.okhttp3.OkHttpSender

import scala.concurrent.ExecutionContext

/**
 * Class for Zipkin tracing at standalone Akka Actor.
 *
 * @param actorSystem an actor system used for tracing
 * @param serviceName a service name (default is `"unknown"`)
 * @param baseUrl a base url of the Zipkin server (default is `"http://localhost:9411"`)
 * @param sampleRate a sampling rate (default is `None` which means `ALWAYS_SAMPLE`)
 */
class ZipkinTraceService(
  actorSystem: ActorSystem,
  serviceName: String = "unknown",
  baseUrl: String = "http://localhost:9411",
  sampleRate: Option[Float] = None) extends ZipkinTraceServiceLike {

  implicit val executionContext: ExecutionContext = actorSystem.dispatchers.lookup(ZipkinTraceConfig.AkkaName)

  private val sender = OkHttpSender.create(baseUrl + "/api/v2/spans")

  val tracing = Tracing.newBuilder()
    .localServiceName(serviceName)
    .spanReporter(AsyncReporter.create(sender))
    .sampler(sampleRate.map(x => Sampler.create(x)) getOrElse Sampler.ALWAYS_SAMPLE)
    .build()

  def close(): Unit = sender.close()

}
