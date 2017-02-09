package jp.co.bizreach.trace.play23.impl

import com.beachape.zipkin.services.{BraveZipkinService, ZipkinServiceLike}
import com.github.kristofa.brave.EmptySpanCollectorImpl
import com.github.kristofa.brave.zipkin.ZipkinSpanCollector
import jp.co.bizreach.trace.service.zipkin.ZipkinTraceServiceLike
import play.api.libs.concurrent.Akka
import play.api.{Configuration, Play}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by nishiyama on 2016/12/09.
  */
object ZipkinTraceService extends ZipkinTraceServiceLike {
  import play.api.Play.current
  implicit val ec: ExecutionContext = Akka.system.dispatchers.lookup(ZipkinConfNames.zipkinAkkaName)
  val conf: Configuration = Play.configuration
  override val service: ZipkinServiceLike = new BraveZipkinService(
    conf.getString(ZipkinConfNames.zipkinServiceHost).getOrElse("dummy"),
    conf.getInt(ZipkinConfNames.zipkinServicePort).getOrElse(0),
    conf.getString(ZipkinConfNames.zipkinServiceName).getOrElse("example"),
    if (conf.getBoolean(ZipkinConfNames.zipkinMock).getOrElse(true)) {
      new EmptySpanCollectorImpl()
    } else {
      new ZipkinSpanCollector(conf.getString(ZipkinConfNames.zipkinHost).getOrElse("localhost"), conf.getInt(ZipkinConfNames.zipkinPort).getOrElse(9410))
    }
  )

  def timeout: FiniteDuration = 50.millis
  val sampleRate: Double = conf.getDouble(ZipkinConfNames.zipkinSampleRate).getOrElse(1.0)
}

object ZipkinConfNames {
  val zipkinAkkaName = "zipkin-trace-context"

  val zipkinServiceHost = "trace.serviceHost"
  val zipkinServicePort = "trace.servicePort"
  val zipkinServiceName = "trace.serviceName"

  val zipkinHost = "trace.zipkin.host"
  val zipkinPort = "trace.zipkin.port"
  val zipkinMock = "trace.zipkin.mock"
  val zipkinSampleRate = "trace.zipkin.sampleRate"
}
