package jp.co.bizreach.trace.play23

import jp.co.bizreach.trace.TraceData
import play.api.Application
import play.api.libs.iteratee.Enumerator
import play.api.libs.ws._

import scala.concurrent.Future

object TraceWS {

  def url(spanName: String, url: String)(implicit app: Application, traceData: TraceData): play.api.libs.ws.WSRequestHolder = {
    new TraceWSRequest(spanName, WS.url(url).withHeaders(ZipkinTraceService.toMap(traceData).toSeq: _*), traceData)
  }

}

class TraceWSRequest(spanName: String, request: WSRequestHolder, traceData: TraceData) extends WSRequestHolder {
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

  override def sign(calc: SignatureCalculator): WSRequestHolder = new TraceWSRequest(spanName, request.sign(calc), traceData)

  override def withAuth(username: String, password: String, scheme: WSAuthScheme): WSRequestHolder = new TraceWSRequest(spanName, request.withAuth(username, password, scheme), traceData)

  override def withHeaders(hdrs: (String, String)*): WSRequestHolder = new TraceWSRequest(spanName, request.withHeaders(hdrs:_*), traceData)

  override def withQueryString(parameters: (String, String)*): WSRequestHolder = new TraceWSRequest(spanName, request.withQueryString(parameters:_*), traceData)

  override def withFollowRedirects(follow: Boolean): WSRequestHolder = new TraceWSRequest(spanName, request.withFollowRedirects(follow), traceData)

  override def withRequestTimeout(timeout: Int): WSRequestHolder = new TraceWSRequest(spanName, request.withRequestTimeout(timeout), traceData)

  override def withVirtualHost(vh: String): WSRequestHolder = new TraceWSRequest(spanName, request.withVirtualHost(vh), traceData)

  override def withProxyServer(proxyServer: WSProxyServer): WSRequestHolder = new TraceWSRequest(spanName, request.withProxyServer(proxyServer), traceData)

  override def withBody(body: WSBody): WSRequestHolder = new TraceWSRequest(spanName, request.withBody(body), traceData)

  override def withMethod(method: String): WSRequestHolder = new TraceWSRequest(spanName, request.withMethod(method), traceData)

  override def execute(): Future[Response] = ZipkinTraceService.traceFuture(spanName){ _ => request.execute() }(traceData)

  override def stream(): Future[(WSResponseHeaders, Enumerator[Array[Byte]])] = request.stream()

}
