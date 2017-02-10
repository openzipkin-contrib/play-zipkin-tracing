import jp.co.bizreach.trace.play23.filter.ZipkinTraceFilter
import play.api.GlobalSettings
import play.api.mvc.WithFilters

/**
  * Created by nishiyama on 2016/12/09.
  */
object Global extends WithFilters(new ZipkinTraceFilter()) with GlobalSettings
