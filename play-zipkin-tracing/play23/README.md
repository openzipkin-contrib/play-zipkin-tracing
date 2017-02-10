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

import jp.co.bizreach.trace.play23.impl.ZipkinTraceService
import jp.co.bizreach.trace.service.{Trace, TracedFuture}
import play.api.libs.ws.WS
import play.api.mvc.{Action, Controller}
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._

class ApiController extends Controller {

  import jp.co.bizreach.trace.play23.implicits.TraceImplicits._
  implicit val traceService = ZipkinTraceService
  
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
