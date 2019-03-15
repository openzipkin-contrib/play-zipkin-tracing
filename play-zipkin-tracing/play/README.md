play-zipkin-tracing-play
========

A library to add tracing capability to Play 2.7 based microservices.

## Setup

Add following dependency to `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "jp.co.bizreach" %% "play-zipkin-tracing-play" % "2.2.0"
)
```

Add following configuration to `application.conf`:

```
play.http.filters=filters.Filters

trace {
  service-name = "zipkin-api-sample"

  zipkin {
    base-url = "http://localhost:9411"
    sample-rate = 0.1
  }
}

zipkin-trace-context {
  fork-join-executor {
    parallelism-factor = 20.0
    parallelism-max = 200
  }
}

play.modules.enabled  += "jp.co.bizreach.trace.play.module.ZipkinModule"
```

## Usage

Inject `ZipkinTraceFilter` to `filter.Filters`:

```scala
package filters

import javax.inject.Inject
import jp.co.bizreach.trace.play.filter.ZipkinTraceFilter
import play.api.http.DefaultHttpFilters

class Filters @Inject() (
  zipkinTraceFilter: ZipkinTraceFilter
) extends DefaultHttpFilters(zipkinTraceFilter)
```

In the controller, trace action and calling another services as following:


```scala
package controllers

import play.api.mvc._
import play.api.libs.json.Json
import jp.co.bizreach.trace.ZipkinTraceServiceLike
import jp.co.bizreach.trace.play.{TraceWSClient, ZipkinTraceService}
import jp.co.bizreach.trace.play.implicits.ZipkinTraceImplicits
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject

class ApiController @Inject()
  (components: ControllerComponents, ws: TraceWSClient, val tracer: ZipkinTraceServiceLike)
  (implicit val ec: ExecutionContext)
    extends AbstractController(components) with ZipkinTraceImplicits {

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
