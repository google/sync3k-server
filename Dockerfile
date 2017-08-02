FROM anapsix/alpine-java

MAINTAINER sync3k

ENV kafkaServer="kafka:9092"

ADD "target/scala-2.12/sync3k-server-assembly-0.0.0-SNAPSHOT.jar" /opt/sync3k-server.jar
RUN chmod 400 /opt/sync3k-server.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar /opt/sync3k-server.jar --kafkaServer $kafkaServer"]
