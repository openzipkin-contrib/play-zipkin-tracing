package jp.co.bizreach.trace.play24.module

import jp.co.bizreach.trace.play25.ZipkinTraceService
import jp.co.bizreach.trace.ZipkinTraceServiceLike
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

/**
 * A Zipkin module.
 *
 * This module can be registered with Play automatically by appending it in application.conf:
 * {{{
 *   play.modules.enabled += "jp.co.bizreach.trace.play25.module.ZipkinModule"
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
