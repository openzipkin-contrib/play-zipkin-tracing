package jp.co.bizreach.trace.service.zipkin

/**
  * Created by nishiyama on 2016/12/16.
  */
object SpanHttpHeaders extends Enumeration {

  val TraceIdHeaderKey = Value("X-B3-TraceId")
  val SpanIdHeaderKey = Value("X-B3-SpanId")
  val ParentIdHeaderKey = Value("X-B3-ParentSpanId")

  val stringValues: Set[String] = values.map(_.toString)
}

object TraceHttpHeaders extends Enumeration {
  val SampledHeaderKey = Value("X-B3-Sampled")

  val stringValues: Set[String] = values.map(_.toString)
}
