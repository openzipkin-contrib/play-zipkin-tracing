play-zipkin-tracing-play25
========

A library to add tracing capability to Play 2.5 based microservices.

## Setup

Add following dependency to `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "jp.co.bizreach" %% "play-zipkin-tracing-play25" % "1.1.0"
)
```

Add following configuration to `application.conf`:

```
play.http.filters=filters.Filters

trace {
  service-name = "zipkin-api-sample"

  zipkin {
    host = "localhost"
    port = 9411
    sample-rate = 0.1
  }
}

zipkin-trace-context {
  fork-join-executor {
    parallelism-factor = 20.0
    parallelism-max = 200
  }
}

play.modules.enabled  += "jp.co.bizreach.trace.play25.module.ZipkinModule"
```

## Usage

Inject `ZipkinTraceFilter` to `filter.Filters`:

```scala
package filters

import javax.inject.Inject
import jp.co.bizreach.trace.play25.filter.ZipkinTraceFilter
import play.api.http.DefaultHttpFilters

class Filters @Inject() (
  zipkinTraceFilter: ZipkinTraceFilter
) extends DefaultHttpFilters(zipkinTraceFilter)
```

In the controller, trace action and calling another services as following:


```scala
package controllers

import play.api.mvc.{Action, Controller}
import play.api.libs.json.Json
import jp.co.bizreach.trace.play25.{TraceWSClient, ZipkinTraceService}
import jp.co.bizreach.trace.play25.implicits.ZipkinTraceImplicits
import scala.concurrent.ExecutionContext
import javax.inject.Inject

class ApiController @Inject()
  (ws: TraceWSClient, val tracer: ZipkinTraceServiceLike)
  (implicit val ec: ExecutionContext)
    extends Controller with ZipkinTraceImplicits {

  // Trace blocking action
  def test1 = Action { implicit request =>
    tracer.trace("sync"){ implicit traceData =>
      println("Hello World!")
      Ok(Json.obj("result" -> "ok"))
    }
  }

  // Trace async action
  def test2 = Action.async { implicit request =>
    tracer.traceFuture("async"){ implicit traceData =>
      Future {
        println("Hello World!")
        Ok(Json.obj("result" -> "ok"))
      }
    }
  }

  // Trace WS request
  def test3 = Action.async { implicit request =>
    ws.url("ws", "http://localhost:9992/api/hello")
      .get().map { res => Ok(res.json) }
  }

}
```
