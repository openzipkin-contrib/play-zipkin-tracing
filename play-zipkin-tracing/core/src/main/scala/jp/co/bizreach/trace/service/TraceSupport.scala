package jp.co.bizreach.trace.service

import scala.concurrent.Future

/**
  * Created by nishiyama on 2016/12/08.
  */
object TracedFuture {
  def apply[A](traceName: String, annotations: (String, String)*)(f: Option[TraceCassette] => Future[A])(implicit parentCassette: TraceCassette, traceServiceLike: TraceServiceLike): Future[A] = {
    traceServiceLike.traceFuture(traceName, parentCassette, annotations)(f)
  }
}

object Trace {
  def apply[A](traceName: String, annotations: (String, String)*)(f: Option[TraceCassette] => A)(implicit parentCassette: TraceCassette, traceServiceLike: TraceServiceLike): A = {
    traceServiceLike.trace(traceName, parentCassette, annotations)(f)
  }
}
