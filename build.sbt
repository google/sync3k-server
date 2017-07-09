lazy val akkaHttpVersion = "10.0.9"
lazy val akkaVersion    = "2.5.3"
lazy val akkaKafkaVersion = "0.16"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "sync3k",
      scalaVersion    := "2.12.2"
    )),
    name := "Sync3k Server",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"         % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"       % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-kafka" % akkaKafkaVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "org.scalatest"     %% "scalatest"         % "3.0.1"         % Test
    )
  )
