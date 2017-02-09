package jp.co.bizreach.trace.play25.module

import jp.co.bizreach.trace.play25.impl.ZipkinTraceService
import jp.co.bizreach.trace.play25.filter.ZipkinTraceFilter
import jp.co.bizreach.trace.play25.impl.ZipkinTraceService
import jp.co.bizreach.trace.service.TraceServiceLike
import jp.co.bizreach.trace.service.zipkin.ZipkinTraceServiceLike
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

/**
  * Created by nishiyama on 2016/12/08.
  */
class ZipkinModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[ZipkinTraceServiceLike].to[ZipkinTraceService],
      bind[TraceServiceLike].to[ZipkinTraceService]
    )
  }
}
