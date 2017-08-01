play-zipkin-tracing [![Build Status](https://travis-ci.org/bizreach/play-zipkin-tracing.svg?branch=master)](https://travis-ci.org/bizreach/play-zipkin-tracing)
========

Provides distributed tracing for Play Framework using [Zipkin](http://zipkin.io/). It makes possible to trace HTTP calls between Play based microservices easily without performance degradation.

## Supported versions

- [Akka 2.5.3](play-zipkin-tracing/akka/README.md)
- [Play 2.6](play-zipkin-tracing/play26/README.md)
- [Play 2.5](play-zipkin-tracing/play25/README.md)
- [Play 2.4](play-zipkin-tracing/play24/README.md)
- [Play 2.3](play-zipkin-tracing/play23/README.md)

## Sample projects

- [zipkin-akka-actor](https://github.com/bizreach/play-zipkin-tracing/tree/master/sample/zipkin-akka-actor)
- [zipkin-api-play26](https://github.com/bizreach/play-zipkin-tracing/tree/master/sample/zipkin-api-play26)
- [zipkin-api-play25](https://github.com/bizreach/play-zipkin-tracing/tree/master/sample/zipkin-api-play25)
- [zipkin-api-play24](https://github.com/bizreach/play-zipkin-tracing/tree/master/sample/zipkin-api-play24)
- [zipkin-api-play23](https://github.com/bizreach/play-zipkin-tracing/tree/master/sample/zipkin-api-play23)

### How to run sample projects

1. Run zipkin-api-play26 project

  ```
  $ cd sample/zipkin-api-play26
  $ sbt run
  ```

2. Run zipkin-api-play24 project

  ```
  $ cd sample/zipkin-api-play24
  $ sbt run
  ```

3. Run Zipkin UI

  ```
  $ java -jar zipkin.jar
  ```

4. Hit http://localhost:9991/nest in some way

  ```
  $ curl http://localhost:9991/nest
  ```

Then you can see traced data on Zipkin UI (http://localhost:9411) as:

![sample](sample.png)
