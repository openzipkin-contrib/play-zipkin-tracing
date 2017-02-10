package filters

import javax.inject.{Inject, Named}

import jp.co.bizreach.trace.play25.filter.ZipkinTraceFilter
import play.api.http.DefaultHttpFilters

class Filters @Inject() (
  traceFilter: ZipkinTraceFilter
) extends DefaultHttpFilters(traceFilter) {

}


