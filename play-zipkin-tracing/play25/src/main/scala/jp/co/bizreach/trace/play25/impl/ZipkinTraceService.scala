package jp.co.bizreach.trace.play25.impl

import javax.inject.Inject

import akka.actor.ActorSystem
import com.beachape.zipkin.services.{BraveZipkinService, ZipkinServiceLike}
import com.github.kristofa.brave.EmptySpanCollectorImpl
import com.github.kristofa.brave.zipkin.ZipkinSpanCollector
import jp.co.bizreach.trace.service.zipkin._
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by nishiyama on 2016/12/08.
  */
class ZipkinTraceService @Inject()(conf: Configuration, actorSystem: ActorSystem) extends ZipkinTraceServiceLike {
  implicit val executionContext: ExecutionContext = actorSystem.dispatchers.lookup(ZipkinTraceConfig.zipkinAkkaName)
  val sampleRate: Double = conf.getDouble(ZipkinTraceConfig.zipkinSampleRate).getOrElse(1.0)
  val timeout: FiniteDuration = 50.millis

  lazy val service: ZipkinServiceLike = new BraveZipkinService(
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
