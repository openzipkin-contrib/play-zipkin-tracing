play-zipkin-tracing-play25
========

A library to add tracing capability to Play 2.5 based microservices.

## Setup

Add following dependency to `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "jp.co.bizreach" %% "play-zipkin-tracing-play25" % "0.0.1-SNAPSHOT"
)
```

Add following configuration to `application.conf`:

```
play.http.filters=filters.Filters

trace {
  serviceHost = "localhost"
  servicePort = "9910"
  serviceName = "zipkin-api-sample"

  zipkin {
    host = "localhost"
    port = 9410
    sampleRate = 0.1
    mock = false
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
import play.api.libs.ws.WSClient
import jp.co.bizreach.trace.service.{TraceServiceLike, TracedFuture, Trace}
import jp.co.bizreach.trace.play25.implicits.TraceImplicits._
import scala.concurrent._
import scala.concurrent.duration._
import javax.inject.Inject

class ApiController @Inject() (ws: WSClient)
  (implicit val traceService: TraceServiceLike, val ec: ExecutionContext) 
    extends Controller {

  // Trace sync action
  def test1 = Action { implicit req =>
    Trace("sync-call"){ implicit cassette =>
      val f = ws.url("http://localhost:9992/api/hello")
        .withTraceHeader() // Call withTraceHeader to set Zipkin headers
        .get().map { res =>
        Ok(res.json)
      }
      Await.result(f, Duration.Inf)
    }
  }

  // Trace async action
  def test2 = Action.async { implicit req =>
    // Use TracedFuture instead of Future
    TracedFuture("async-call"){ implicit cassette =>
      ws.url("http://localhost:9992/api/hello")
        .withTraceHeader() // Call withTraceHeader to set Zipkin headers
        .get().map { res =>
        Ok(res.json)
      }
    }
  }
  
}
```
