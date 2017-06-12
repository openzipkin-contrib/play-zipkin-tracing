package jp.co.bizreach.trace.play26.module

import jp.co.bizreach.trace.ZipkinTraceServiceLike
import jp.co.bizreach.trace.play26.ZipkinTraceService
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

/**
 * A Zipkin module.
 *
 * This module can be registered with Play automatically by appending it in application.conf:
 * {{{
 *   play.modules.enabled += "jp.co.bizreach.trace.play26.module.ZipkinModule"
 * }}}
 *
 */
class ZipkinModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[ZipkinTraceServiceLike].to[ZipkinTraceService]
    )
  }
}
