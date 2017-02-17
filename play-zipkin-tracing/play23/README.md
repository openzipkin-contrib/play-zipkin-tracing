play-zipkin-tracing-play23
========

A library to add tracing capability to Play 2.3 based microservices.

## Setup

Add following dependency to `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "jp.co.bizreach" %% "play-zipkin-tracing-play23" % "0.0.1-SNAPSHOT"
)
```

Add following configuration to `application.conf`:

```
trace {
  service-name = "zipkin-api-sample"

  zipkin {
    host = "localhost"
    port = 9410
    sample-rate = 0.1
  }
}

zipkin-trace-context {
  fork-join-executor {
    parallelism-factor = 20.0
    parallelism-max = 200
  }
}
```

## Usage

Create `Global` object with `ZipkinTraceFilter` as following and put it into the classpath root.

```scala
import com.stanby.trace.play23.filter.ZipkinTraceFilter
import play.api.GlobalSettings
import play.api.mvc.WithFilters

object Global extends WithFilters(new ZipkinTraceFilter()) with GlobalSettings
```

In the controller, trace action and calling another services as following:


```scala
package controllers

import jp.co.bizreach.trace.play23.TraceWS
import jp.co.bizreach.trace.play23.implicits.ZipkinTraceImplicits
import play.api.mvc.{Action, Controller}
import play.api.Play.current
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global

class ApiController extends Controller with ZipkinTraceImplicits {

  // Trace blocked action
  def test1 = Action { implicit request =>
    ZipkinTraceService.trace("sync"){
      println("Hello World!")
      Ok(Json.obj("api" -> "once"))
    }
  }

  // Trace async action
  def test2 = Action.async { implicit request =>
    ZipkinTraceService.traceFuture("async"){
      Future {
        println("Hello World!")
        Ok(Json.obj("api" -> "once"))
      }
    }
  }

  // Trace WS request
  def test3 = Action.async { implicit req =>
    TraceWS.url("ws", "http://localhost:9992/api/hello")
      .get().map { res => Ok(res.json) }
  }

}
```
