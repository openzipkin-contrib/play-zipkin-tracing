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
    tracer.trace("local-wait"){
      //Thread.sleep(300 + Random.nextInt(700))
      Thread.sleep(500)
      println("** end **")
    }
    println("** send **")
    repo.call(url)
  }
}
