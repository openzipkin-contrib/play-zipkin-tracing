package jp.co.bizreach.trace.play26

import java.io.{File, IOException}
import java.net.URI
import javax.inject.Inject

import akka.stream.scaladsl.Source
import akka.util.ByteString
import jp.co.bizreach.trace.{TraceData, ZipkinTraceServiceLike}
import play.api.libs.ws._
import play.api.mvc.MultipartFormData.Part

import scala.concurrent.Future
import scala.concurrent.duration.Duration

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

}

private class TraceWSRequest(spanName: String, request: WSRequest, tracer: ZipkinTraceServiceLike, traceData: TraceData) extends WSRequest {

  override type Self = TraceWSRequest
  override type Response = WSResponse

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

  override def sign(calc: WSSignatureCalculator): TraceWSRequest = new TraceWSRequest(spanName, request.sign(calc), tracer, traceData)
  override def withAuth(username: String, password: String, scheme: WSAuthScheme): TraceWSRequest = new TraceWSRequest(spanName, request.withAuth(username, password, scheme), tracer, traceData)
  @deprecated("Use withHttpHeaders or addHttpHeaders", "1.0.0")
  override def withHeaders(hdrs: (String, String)*): TraceWSRequest = new TraceWSRequest(spanName, request.withHeaders(hdrs:_*), tracer, traceData)
  @deprecated("Use withQueryStringParameters or addQueryStringParameter", "1.0.0")
  override def withQueryString(parameters: (String, String)*): TraceWSRequest = new TraceWSRequest(spanName, request.withQueryString(parameters:_*), tracer, traceData)
  override def withFollowRedirects(follow: Boolean): TraceWSRequest = new TraceWSRequest(spanName, request.withFollowRedirects(follow), tracer, traceData)
  override def withRequestTimeout(timeout: Duration): TraceWSRequest = new TraceWSRequest(spanName, request.withRequestTimeout(timeout), tracer, traceData)
  override def withRequestFilter(filter: WSRequestFilter): TraceWSRequest = new TraceWSRequest(spanName, request.withRequestFilter(filter), tracer, traceData)
  override def withVirtualHost(vh: String): TraceWSRequest = new TraceWSRequest(spanName, request.withVirtualHost(vh), tracer, traceData)
  override def withProxyServer(proxyServer: WSProxyServer): TraceWSRequest = new TraceWSRequest(spanName, request.withProxyServer(proxyServer), tracer, traceData)
  override def withBody(body: WSBody): TraceWSRequest = new TraceWSRequest(spanName, request.withBody(body), tracer, traceData)
  override def withMethod(method: String): TraceWSRequest = new TraceWSRequest(spanName, request.withMethod(method), tracer, traceData)

  override def execute(): Future[WSResponse] = tracer.traceWS(spanName, traceData){ span =>
    request.withHeaders(tracer.toMap(span).toSeq: _*).execute()
  }
  override def stream(): Future[StreamedResponse] = tracer.traceWS(spanName, traceData){ span =>
    request.withHeaders(tracer.toMap(span).toSeq: _*).stream()
  }

  override def withBody(body: Source[Part[Source[ByteString, _]], _]): TraceWSRequest = new TraceWSRequest(spanName, request.withBody(body), tracer, traceData)
  override def uri: URI = request.uri
  override def contentType: Option[String] = request.contentType
  override def withBody(file: File): TraceWSRequest = new TraceWSRequest(spanName, request.withBody(file), tracer, traceData)
  override def withBody[T](body: T)(implicit evidence$1: BodyWritable[T]): TraceWSRequest = new TraceWSRequest(spanName, request.withBody(body), tracer, traceData)

  override def patch(body: Source[Part[Source[ByteString, _]], _]): Future[WSResponse] = withBody(body).execute("PATCH")
  override def patch[T](body: T)(implicit evidence$2: BodyWritable[T]): Future[WSResponse] = withBody(body).execute("PATCH")
  override def patch(body: File): Future[WSResponse] = withBody(body).execute("PATCH")

  override def get(): Future[WSResponse] = execute("GET")

  override def post[T](body: T)(implicit evidence$3: BodyWritable[T]): Future[WSResponse] = withBody(body).execute("POST")
  override def post(body: File): Future[WSResponse] = withBody(body).execute("POST")
  override def post(body: Source[Part[Source[ByteString, _]], _]): Future[WSResponse] = withBody(body).execute("POST")

  override def put[T](body: T)(implicit evidence$4: BodyWritable[T]): Future[WSResponse] = withBody(body).execute("PUT")
  override def put(body: File): Future[WSResponse] = withBody(body).execute("PUT")
  override def put(body: Source[Part[Source[ByteString, _]], _]): Future[WSResponse] = withBody(body).execute("PUT")

  override def delete(): Future[WSResponse] = execute("DELETE")
  override def head(): Future[WSResponse] = execute("HEAD")
  override def options(): Future[WSResponse] = execute("OPTIONS")
  override def execute(method: String): Future[WSResponse] = withMethod(method).execute()

  override def cookies: Seq[WSCookie] = request.cookies
  override def withHttpHeaders(headers: (String, String)*): TraceWSRequest = new TraceWSRequest(spanName, request.withHttpHeaders(headers:_*), tracer, traceData)
  override def withQueryStringParameters(parameters: (String, String)*): TraceWSRequest = new TraceWSRequest(spanName, request.withQueryStringParameters(parameters:_*), tracer, traceData)
  override def withCookies(cookies: WSCookie*): TraceWSRequest = new TraceWSRequest(spanName, request.withCookies(cookies:_*), tracer, traceData)
}

