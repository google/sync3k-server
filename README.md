# Sync3k Server

`sync3k-server` is a lightweight websocket gateway to kafka. The server accepts websocket path:

`ws://localhost:8080/kafka/:topic/:offset`

Network binding, listening port and kafka bootstrap server can be configured through flags.

```
  -b, --bind <value>       interface to bind to. Defaults to 0.0.0.0
  -p, --port <value>       port number to listen to. Defaults to 8080
  -k, --kafkaServer <value>
                           Kafka bootstrap server. Defaults to localhost:9092
```

The server is intended to be used with [sync3k-client](https://github.com/google/sync3k-client).

## Usage

Use `sbt` to run the server.

```sh
sbt "run --port 8080 --kafkaServer kafkaserver:9092"
```

## Running everything with Docker

`sync3k-server` includes [Dockerfile](Dockerfile) and [docker compose YAML](docker-compose.yml) that launches `zookeeper`, `kafka` and `sync3k-server`.

To build the Docker image, first build an uber-jar with sbt assembly command:

```sh
sbt assembly
```

Then build the Docker image:

```sh
docker build -t sync3k-server .
```

Finally, launch everything with `docker-compose`:

```sh
docker-compose up -d
```

Disclaimer: This is not an official Google product.
