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

## Usage

Use `sbt` to run the server.

```sh
sbt "run --port 8080 --kafkaServer kafkaserver:9092"
```

Disclaimer: This is not an official Google product.
