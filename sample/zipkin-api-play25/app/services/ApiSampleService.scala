package services

import javax.inject.Inject

import jp.co.bizreach.trace.{TraceData, ZipkinTraceServiceLike}
import repositories.ApiRepository

import scala.concurrent.Future
import scala.util.Random

/**
  * Created by nishiyama on 2016/12/05.
  */
class ApiSampleService @Inject() (
  repo: ApiRepository,
  tracer: ZipkinTraceServiceLike
) {

  def sample(url: String)(implicit traceData: TraceData): Future[String] = {
    tracer.trace("local-wait-1"){ implicit traceData =>
      tracer.trace("local-wait-2"){ implicit traceData =>
        Thread.sleep(300 + Random.nextInt(700))
      }
      repo.call(url)
    }
  }
}
