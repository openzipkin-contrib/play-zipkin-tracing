package jp.co.bizreach.trace.play25

import javax.inject.Inject

import akka.actor.ActorSystem
import brave.Tracer
import jp.co.bizreach.trace.zipkin.{ZipkinTraceServiceLike, ZipkinTraceConfig}
import play.api.Configuration
import zipkin.reporter.AsyncReporter
import zipkin.reporter.okhttp3.OkHttpSender

import scala.concurrent.ExecutionContext

/**
  * Created by nishiyama on 2016/12/08.
  */
class ZipkinTraceService @Inject() (
  //val api: implicits.ZipkinTraceImplicits,
  conf: Configuration,
  actorSystem: ActorSystem) extends ZipkinTraceServiceLike {

  implicit val executionContext: ExecutionContext = actorSystem.dispatchers.lookup(ZipkinTraceConfig.zipkinAkkaName)

  val tracer = Tracer.newBuilder()
    .localServiceName(conf.getString(ZipkinTraceConfig.zipkinServiceName) getOrElse "example")
    .reporter(AsyncReporter
      .builder(OkHttpSender.create(
        s"http://${conf.getString(ZipkinTraceConfig.zipkinHost) getOrElse "localhost"}:${conf.getInt(ZipkinTraceConfig.zipkinPort) getOrElse 9410}/api/v1/spans"
      ))
      .build()
    )
    .build()

}
