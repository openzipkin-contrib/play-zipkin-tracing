package jp.co.bizreach.trace.service.zipkin

/**
  * Created by nishiyama on 2016/12/08.
  */
case class ZipkinTraceConfig(
  serviceName: String,
  serviceHost: String,
  servicePort: Int,
  zipkinHost: String,
  zipkinPort: Int,
  zipkinMock: Boolean
)
