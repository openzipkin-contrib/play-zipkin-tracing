package jp.co.bizreach.trace.service.zipkin

import com.twitter.zipkin.gen.Span
import jp.co.bizreach.trace.service.TraceCassette

/**
  * Created by nishiyama on 2016/12/08.
  */
case class ZipkinTraceCassette(span: Span, sampled: Boolean = false) extends TraceCassette

