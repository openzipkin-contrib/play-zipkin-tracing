package services

import javax.inject.Inject

import jp.co.bizreach.trace.{TraceService, TraceCassette}
import repositories.ApiRepository

import scala.concurrent.Future
import scala.util.Random

/**
  * Created by nishiyama on 2016/12/05.
  */
class ApiSampleService @Inject() (
  repo: ApiRepository,
  tracer: TraceService
) {

  def sample(url: String)(implicit traceCassette: TraceCassette): Future[String] = {
    tracer.trace("local-wait"){ implicit cassette =>
      Thread.sleep(300 + Random.nextInt(700))
    }

    repo.call(url)
  }
}
