package filters


import javax.inject.Inject

import jp.co.bizreach.trace.play24.filter.ZipkinTraceFilter
import play.api.http.HttpFilters

class Filters @Inject() (
  zipkinTraceFilter: ZipkinTraceFilter
) extends HttpFilters {
  val filters = Seq(zipkinTraceFilter)
}
