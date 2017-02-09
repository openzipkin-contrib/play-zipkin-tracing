package jp.co.bizreach.trace.play25.impl

import javax.inject.Inject

import akka.actor.ActorSystem
import com.beachape.zipkin.services.{BraveZipkinService, ZipkinServiceLike}
import com.github.kristofa.brave.EmptySpanCollectorImpl
import com.github.kristofa.brave.zipkin.ZipkinSpanCollector
import jp.co.bizreach.trace.service.zipkin.ZipkinTraceServiceLike
import jp.co.bizreach.trace.service.zipkin.ZipkinTraceServiceLike
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

/**
  * Created by nishiyama on 2016/12/08.
  */
class ZipkinTraceService @Inject()(conf: Configuration, actorSystem: ActorSystem) extends ZipkinTraceServiceLike {
  implicit val ec: ExecutionContext = actorSystem.dispatchers.lookup(ZipkinConfNames.zipkinAkkaName)
  val sampleRate: Double = conf.getDouble(ZipkinConfNames.zipkinSampleRate).getOrElse(1.0)
  val timeout: FiniteDuration = 50.millis

  lazy val service: ZipkinServiceLike = new BraveZipkinService(
    conf.getString(ZipkinConfNames.zipkinServiceHost).getOrElse("dummy"),
    conf.getInt(ZipkinConfNames.zipkinServicePort).getOrElse(0),
    conf.getString(ZipkinConfNames.zipkinServiceName).getOrElse("example"),
    if (conf.getBoolean(ZipkinConfNames.zipkinMock).getOrElse(true)) {
      new EmptySpanCollectorImpl()
    } else {
      Try(
        new ZipkinSpanCollector(conf.getString(ZipkinConfNames.zipkinHost).getOrElse("localhost"), conf.getInt(ZipkinConfNames.zipkinPort).getOrElse(9410))
      ).getOrElse(new EmptySpanCollectorImpl())
    }
  )

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
