package services

import javax.inject.Inject

import brave.play.{TraceData, ZipkinTraceServiceLike}
import repositories.ApiRepository

import scala.concurrent.Future

class ApiSampleService @Inject() (
  repo: ApiRepository,
  tracer: ZipkinTraceServiceLike
) {
  def sample(url: String)(implicit traceData: TraceData): Future[String] = {
    tracer.trace("local-wait"){ implicit traceData =>
      //Thread.sleep(300 + Random.nextInt(700))
      Thread.sleep(500)
      println("** end **")
    }
    println("** send **")
    repo.call(url)
  }
}
