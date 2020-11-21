# play-zipkin-tracing

[![Gitter chat](http://img.shields.io/badge/gitter-join%20chat%20%E2%86%92-brightgreen.svg)](https://gitter.im/openzipkin/zipkin)
[![Build Status](https://github.com/openzipkin-contrib/play-zipkin-tracing/workflows/test/badge.svg)](https://github.com/openzipkin-contrib/play-zipkin-tracing/actions?query=workflow%3Atest)
[![Maven Central](https://img.shields.io/maven-central/v/io.zipkin.brave.play/play-zipkin-tracing-play_2.12.svg)](https://search.maven.org/search?q=g:io.zipkin.brave.play%20AND%20a:play-zipkin-tracing-play_2.12)

Provides distributed tracing for Play Framework using [Zipkin](https://zipkin.io/). It makes possible to trace HTTP calls between Play based microservices easily without performance degradation.

## Supported versions

- [Akka 2.5.x](akka/README.md) (Zipkin1 and Zipkin2 support are available)
- [Play 2.7](play/README.md) (Zipkin1 and Zipkin2 support are available)

## Sample projects

- [zipkin-akka-actor](sample/zipkin-akka-actor) (Zipkin2)
- [zipkin-api-play27](sample/zipkin-api-play27) (Zipkin2)

### How to run sample projects


1. Start Zipkin

```bash
$ curl -sSL https://zipkin.io/quickstart.sh | bash -s
$ java -jar zipkin.jar
```

2. Run zipkin-api-play27 project

```bash
$ cd sample/zipkin-api-play27
$ sbt run
```

3. Run zipkin-api-play24 project

```bash
$ git checkout 1.2.0 -- sample/zipkin-api-play24
$ cd sample/zipkin-api-play24
$ sbt run
```

4. Hit http://localhost:9991/nest in some way

```bash
$ curl http://localhost:9991/nest
```

Then you can see traced data on Zipkin UI (http://localhost:9411/zipkin) as:

!![Zipkin Screen Shot](https://user-images.githubusercontent.com/64215/99867404-281dfa80-2bf4-11eb-83b0-03275a363712.png)

## Artifacts
All artifacts publish to the group ID "io.zipkin.brave.play". We use a common release version for
all components.

### Library Releases
Releases are at [Sonatype](https://oss.sonatype.org/content/repositories/releases) and [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.zipkin.brave.play%22)

### Library Snapshots
Snapshots are uploaded to [Sonatype](https://oss.sonatype.org/content/repositories/snapshots) after
commits to master.
