package filters

import javax.inject.Inject

import brave.play.filter.ZipkinTraceFilter
import play.api.http.DefaultHttpFilters

class Filters @Inject() (
  traceFilter: ZipkinTraceFilter
) extends DefaultHttpFilters(traceFilter) {

}
