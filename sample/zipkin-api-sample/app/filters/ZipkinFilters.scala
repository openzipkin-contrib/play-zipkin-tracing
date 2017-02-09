package filters


import javax.inject.Inject

import com.stanby.trace.play25.filter.ZipkinTraceFilter
import play.api.http.DefaultHttpFilters

class Filters @Inject() (
  zipkinTraceFilter: ZipkinTraceFilter
) extends DefaultHttpFilters(zipkinTraceFilter) {

}
