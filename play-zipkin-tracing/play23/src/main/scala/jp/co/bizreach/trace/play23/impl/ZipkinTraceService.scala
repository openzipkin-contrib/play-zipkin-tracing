package jp.co.bizreach.trace.play23.impl

import brave.Tracer
import jp.co.bizreach.trace.service.zipkin._
import play.api.libs.concurrent.Akka
import play.api.{Configuration, Play}
import zipkin.reporter.AsyncReporter
import zipkin.reporter.okhttp3.OkHttpSender

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by nishiyama on 2016/12/09.
  */
object ZipkinTraceService extends ZipkinTraceServiceLike {
  import play.api.Play.current
  val conf: Configuration = Play.configuration

  implicit val executionContext: ExecutionContext = Akka.system.dispatchers.lookup(ZipkinTraceConfig.zipkinAkkaName)
  val timeout: FiniteDuration = 50.millis

  val reporter = AsyncReporter
    .builder(OkHttpSender.create(
      s"http://${conf.getString(ZipkinTraceConfig.zipkinHost) getOrElse "localhost"}:${conf.getInt(ZipkinTraceConfig.zipkinPort) getOrElse 9410}/api/v1/spans"
    ))
    .build()
  val clientTracer = Tracer.newBuilder()
    .localServiceName(conf.getString(ZipkinTraceConfig.zipkinServiceName) getOrElse "example")
    .reporter(reporter)
    .build()

}
