package jp.co.bizreach.trace.play24

import java.io.IOException
import javax.inject.Inject

import jp.co.bizreach.trace.{TraceData, ZipkinTraceServiceLike}
import play.api.libs.iteratee.Enumerator
import play.api.libs.ws._

import scala.concurrent.Future

/**
 * Inject this class instead of WSClient to trace service calling.
 */
class TraceWSClient @Inject()(ws: WSClient, tracer: ZipkinTraceServiceLike) {

  def underlying[T]: T = ws.underlying

  /**
   * Generates a request holder without tracing.
   */
  def url(url: String): WSRequest = ws.url(url)

  /**
   * Generates a request holder with the span name which is used for tracing.
   * B3 headers are added to the generated request holder and tracing data is sent to the zipkin server automatically.
   */
  def url(spanName: String, url: String)(implicit traceData: TraceData): WSRequest = {
    new TraceWSRequest(spanName, ws.url(url), tracer, traceData)
  }

  @scala.throws[IOException]
  def close(): Unit = ws.close()

  private class TraceWSRequest(spanName: String, request: WSRequest, tracer: ZipkinTraceServiceLike, traceData: TraceData) extends WSRequest {

    override val url: String = request.url
    override val method: String = request.method
    override val body: WSBody = request.body
    override val headers: Map[String, Seq[String]] = request.headers
    override val queryString: Map[String, Seq[String]] = request.queryString
    override val calc: Option[WSSignatureCalculator] = request.calc
    override val auth: Option[(String, String, WSAuthScheme)] = request.auth
    override val followRedirects: Option[Boolean] = request.followRedirects
    override val requestTimeout: Option[Int] = request.requestTimeout
    override val virtualHost: Option[String] = request.virtualHost
    override val proxyServer: Option[WSProxyServer] = request.proxyServer

    override def sign(calc: WSSignatureCalculator): WSRequest = new TraceWSRequest(spanName, request.sign(calc), tracer, traceData)
    override def withAuth(username: String, password: String, scheme: WSAuthScheme): WSRequest = new TraceWSRequest(spanName, request.withAuth(username, password, scheme), tracer, traceData)
    override def withHeaders(hdrs: (String, String)*): WSRequest = new TraceWSRequest(spanName, request.withHeaders(hdrs:_*), tracer, traceData)
    override def withQueryString(parameters: (String, String)*): WSRequest = new TraceWSRequest(spanName, request.withQueryString(parameters:_*), tracer, traceData)
    override def withFollowRedirects(follow: Boolean): WSRequest = new TraceWSRequest(spanName, request.withFollowRedirects(follow), tracer, traceData)
    override def withRequestTimeout(timeout: Long): WSRequest = new TraceWSRequest(spanName, request.withRequestTimeout(timeout), tracer, traceData)
    override def withVirtualHost(vh: String): WSRequest = new TraceWSRequest(spanName, request.withVirtualHost(vh), tracer, traceData)
    override def withProxyServer(proxyServer: WSProxyServer): WSRequest = new TraceWSRequest(spanName, request.withProxyServer(proxyServer), tracer, traceData)
    override def withBody(body: WSBody): WSRequest = new TraceWSRequest(spanName, request.withBody(body), tracer, traceData)
    override def withMethod(method: String): WSRequest = new TraceWSRequest(spanName, request.withMethod(method), tracer, traceData)

    override def execute(): Future[WSResponse] = tracer.traceWSFuture(spanName, traceData){ span =>
      request.withHeaders(tracer.toMap(span).toSeq: _*).execute()
    }
    override def stream(): Future[(WSResponseHeaders, Enumerator[Array[Byte]])] = tracer.traceWSFuture(spanName, traceData){ span =>
      request.withHeaders(tracer.toMap(span).toSeq: _*).stream()
    }
  }
}

