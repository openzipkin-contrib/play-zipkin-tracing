package jp.co.bizreach.trace.play23.impl

import com.beachape.zipkin.services.{BraveZipkinService, ZipkinServiceLike}
import com.github.kristofa.brave.EmptySpanCollectorImpl
import com.github.kristofa.brave.zipkin.ZipkinSpanCollector
import jp.co.bizreach.trace.service.zipkin._
import play.api.libs.concurrent.Akka
import play.api.{Configuration, Play}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by nishiyama on 2016/12/09.
  */
object ZipkinTraceService extends ZipkinTraceServiceLike {
  import play.api.Play.current
  val conf: Configuration = Play.configuration

  implicit val executionContext: ExecutionContext = Akka.system.dispatchers.lookup(ZipkinTraceConfig.zipkinAkkaName)
  val sampleRate: Double = conf.getDouble(ZipkinTraceConfig.zipkinSampleRate).getOrElse(1.0)
  val timeout: FiniteDuration = 50.millis

  override val service: ZipkinServiceLike = new BraveZipkinService(
    conf.getString(ZipkinTraceConfig.zipkinServiceHost).getOrElse("dummy"),
    conf.getInt(ZipkinTraceConfig.zipkinServicePort).getOrElse(0),
    conf.getString(ZipkinTraceConfig.zipkinServiceName).getOrElse("example"),
    if (conf.getBoolean(ZipkinTraceConfig.zipkinMock).getOrElse(true)) {
      new EmptySpanCollectorImpl()
    } else {
      new ZipkinSpanCollector(conf.getString(ZipkinTraceConfig.zipkinHost).getOrElse("localhost"), conf.getInt(ZipkinTraceConfig.zipkinPort).getOrElse(9410))
    }
  )

}
