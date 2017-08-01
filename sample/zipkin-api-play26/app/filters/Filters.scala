package filters

import javax.inject.Inject

import jp.co.bizreach.trace.play26.filter.ZipkinTraceFilter
import play.api.http.DefaultHttpFilters

class Filters @Inject() (
  traceFilter: ZipkinTraceFilter
) extends DefaultHttpFilters(traceFilter) {

}


