Bitcoin exchange rate API
-------------------
# Abstract
This API provides the exchange rate from Bitcoin to US-Dollar (1 Bitcoin = x USD) which is crawled in a configurable interval.

# Architecture
The application is written in [Play](https://www.playframework.com/) and scaffolded via the [Giter8](http://www.foundweekends.org/giter8/) template [ldaume/play-restful-docker.g8](https://github.com/ldaume/play-restful-docker.g8)

It contains a [swagger](https://swagger.io/) specification for easy usage and the crawler is scheduled with [akka](https://akka.io/) which calls a public API in a configurable interval.

Every exchange rate is stored in a [ArangoDB](https://www.arangodb.com/) collection.

# Usage
## Build Docker image
### sbt
One can build a docker image with [sbt](https://www.scala-sbt.org/). 

1. build swagger docs: `sbt swagger`
2. Add a application secret `sbt playUpdateSecret`
3. Just type `sbt docker:publishLocal`

### Drone CI
One can use the CLI of [Drone CI](https://drone.io/) by typing `drone exec --trusted`

## Run Application
Just start the application with [docker compose](https://docs.docker.com/compose) by typing `docker-compose -f scripts/docker-compose.yml up`.
The [ArangoDB](https://www.arangodb.com/) will be started as well as the app on port `9060`.
### How to use the application
Just run the application and go to the living documentation at [http://localhost:9060/docs](http://localhost:9060/docs).

# Configuration
## Crawler
The initial crawl delay and interval can be configured with the environment variables `CRAWLER_INITIAL_DELAY` and `CRAWLER_INTERVAL`.

Possible values:

x...
* ns, nanosecond, nanoseconds
* us, microsecond, microseconds
* ms, millisecond, milliseconds
* s, second, seconds
* m, minute, minutes
* h, hour, hours
* d, day, days

## Database
The ArangoDB settings can be passed by the ENV's `ARANGODB_HOST`, ` ARANGODB_USERNAME` and `ARANGODB_PASSWORD`.
