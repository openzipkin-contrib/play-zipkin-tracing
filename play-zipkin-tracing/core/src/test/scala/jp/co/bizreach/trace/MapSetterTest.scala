package jp.co.bizreach.trace

import brave.propagation.Propagation
import brave.test.propagation.PropagationSetterTest

/** Tests [[jp.co.bizreach.trace.ZipkinTraceServiceLike.mapSetter]] is a valid setter */
class MapSetterTest extends PropagationSetterTest[collection.mutable.Map[String, String], String] {

  import collection.JavaConverters._

  override val keyFactory = Propagation.KeyFactory.STRING
  override val carrier = collection.mutable.Map[String, String]()

  override def setter = ZipkinTraceServiceLike.mapSetter

  override def read(carrier: collection.mutable.Map[String, String], key: String) =
    carrier.get(key).toList.asJava
}
