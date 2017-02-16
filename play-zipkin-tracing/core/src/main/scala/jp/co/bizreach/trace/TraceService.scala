package jp.co.bizreach.trace

import scala.concurrent.Future

/**
  * Created by nishiyama on 2016/12/08.
  */
trait TraceService {
  val api: TraceImplicits

  def trace[A](traceName: String, tags: (String, String)*)(f: TraceCassette => A)(implicit parentCassette: TraceCassette): A

  def traceFuture[A](traceName: String, tags: (String, String)*)(f: TraceCassette => Future[A])(implicit parentCassette: TraceCassette): Future[A]
}

trait TraceCassette

trait TraceImplicits
