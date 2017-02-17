package jp.co.bizreach.trace.play25

import java.io.IOException
import javax.inject.Inject

import jp.co.bizreach.trace.{TraceData, ZipkinTraceServiceLike}
import play.api.libs.iteratee.Enumerator
import play.api.libs.ws._

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
 * Created by naoki.takezoe on 2017/02/17.
 */
class TraceWSClient @Inject()(ws: WSClient, tracer: ZipkinTraceServiceLike) {

  def underlying[T]: T = ws.underlying

  def url(url: String): WSRequest = ws.url(url)

  def url(spanName: String, url: String)(implicit traceData: TraceData): WSRequest = {
    new TraceWSRequest(spanName, ws.url(url), tracer, traceData).withHeaders(tracer.toMap(traceData).toSeq: _*)
  }

  @scala.throws[IOException]
  def close(): Unit = ws.close()

}

class TraceWSRequest(spanName: String, request: WSRequest, tracer: ZipkinTraceServiceLike, traceData: TraceData) extends WSRequest {

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

  override def withRequestTimeout(timeout: Duration): WSRequest = new TraceWSRequest(spanName, request.withRequestTimeout(timeout), tracer, traceData)

  override def withRequestFilter(filter: WSRequestFilter): WSRequest = new TraceWSRequest(spanName, request.withRequestFilter(filter), tracer, traceData)

  override def withVirtualHost(vh: String): WSRequest = new TraceWSRequest(spanName, request.withVirtualHost(vh), tracer, traceData)

  override def withProxyServer(proxyServer: WSProxyServer): WSRequest = new TraceWSRequest(spanName, request.withProxyServer(proxyServer), tracer, traceData)

  override def withBody(body: WSBody): WSRequest = new TraceWSRequest(spanName, request.withBody(body), tracer, traceData)

  override def withMethod(method: String): WSRequest = new TraceWSRequest(spanName, request.withMethod(method), tracer, traceData)

  override def execute(): Future[WSResponse] = tracer.traceFuture(spanName){ _ => request.execute() }(traceData)

  override def stream(): Future[StreamedResponse] = request.stream()

  @scala.deprecated("Use `WS.stream()` instead.")
  override def streamWithEnumerator(): Future[(WSResponseHeaders, Enumerator[Array[Byte]])] = request.streamWithEnumerator()

}
