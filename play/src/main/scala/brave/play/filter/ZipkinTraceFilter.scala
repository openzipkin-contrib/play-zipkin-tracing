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
package brave.play.filter

import akka.stream.Materializer
import brave.play.ZipkinTraceServiceLike
import javax.inject.Inject
import play.api.mvc.{Filter, Headers, RequestHeader, Result}
import play.api.routing.Router

import scala.concurrent.Future
import scala.util.Failure

/**
 * A Zipkin filter.
 *
 * This filter is that reports how long a request takes to execute in Play as a server span.
 * The way to use this filter is following:
 * {{{
 * class Filters @Inject() (
 *   zipkin: ZipkinTraceFilter
 * ) extends DefaultHttpFilters(zipkin)
 * }}}
 *
 * @param tracer a Zipkin tracer
 * @param mat a materializer
 */
class ZipkinTraceFilter @Inject() (tracer: ZipkinTraceServiceLike)(implicit val mat: Materializer) extends Filter {

  import tracer.executionContext
  private val reqHeaderToSpanName: RequestHeader => String = ZipkinTraceFilter.ParamAwareRequestNamer

  def apply(nextFilter: RequestHeader => Future[Result])(req: RequestHeader): Future[Result] = {
    val serverSpan = tracer.serverReceived(
      spanName = reqHeaderToSpanName(req),
      span = tracer.newSpan(req.headers)((headers, key) => headers.get(key))
    )
    val result = nextFilter(req.withHeaders(new Headers(
      (req.headers.toMap.mapValues(_.headOption getOrElse "") ++ tracer.toMap(serverSpan)).toSeq
    )))
    result.onComplete {
      case Failure(t) => tracer.serverSend(serverSpan, "failed" -> s"Finished with exception: ${t.getMessage}")
      case _ => tracer.serverSend(serverSpan)
    }
    result
  }
}

object ZipkinTraceFilter {
  val ParamAwareRequestNamer: RequestHeader => String = { reqHeader =>
    import org.apache.commons.lang3.StringUtils
    val pathPattern = StringUtils.replace(
      reqHeader.attrs.get(Router.Attrs.HandlerDef).map(_.path).getOrElse(reqHeader.path),
      "<[^/]+>", ""
    )
    s"${reqHeader.method} - $pathPattern"
  }
}
