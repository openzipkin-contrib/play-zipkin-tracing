package jp.co.bizreach.trace.zipkin

import jp.co.bizreach.trace.TraceCassette

/**
  * Created by nishiyama on 2016/12/08.
  */
case class ZipkinTraceCassette(span: brave.Span) extends TraceCassette

