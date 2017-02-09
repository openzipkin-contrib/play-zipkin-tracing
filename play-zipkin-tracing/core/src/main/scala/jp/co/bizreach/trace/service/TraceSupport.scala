package jp.co.bizreach.trace.service

import scala.concurrent.Future

/**
  * Created by nishiyama on 2016/12/08.
  */
object TracedFuture {
  def apply[A](traceName: String, annotations: (String, String)*)(f: Option[TraceCassette] => Future[A])(implicit parentCassete: TraceCassette, traceServiceLike: TraceServiceLike): Future[A] = {
    traceServiceLike.traceFuture(traceName, parentCassete, annotations)(f)
  }
}

object Trace {
  def apply[A](traceName: String, annotations: (String, String)*)(f: Option[TraceCassette] => A)(implicit parentCassete: TraceCassette, traceServiceLike: TraceServiceLike): A = {
    traceServiceLike.trace(traceName, parentCassete, annotations)(f)
  }
}
