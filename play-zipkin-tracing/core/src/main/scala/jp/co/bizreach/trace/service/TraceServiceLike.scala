package jp.co.bizreach.trace.service

import scala.concurrent.Future

/**
  * Created by nishiyama on 2016/12/08.
  */
trait TraceServiceLike {
  def toMap(cassette: TraceCassette): Map[String, String]

  def trace[A](traceName: String, parentCassete: TraceCassette, annotations: Seq[(String, String)])(f: Option[TraceCassette] => A): A

  def traceFuture[A](traceName: String, parentCassette: TraceCassette ,annotations: Seq[(String, String)])(f: Option[TraceCassette] => Future[A]): Future[A]
}
