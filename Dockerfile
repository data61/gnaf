# Build
FROM ubuntu:20.04 as builder

ENV DEBIAN_FRONTEND=noninteractive

WORKDIR /

RUN apt-get update -qq && apt-get -y install apt-transport-https ca-certificates curl gnupg

# https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Linux.html#Ubuntu+and+other+Debian-based+distributions
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add

RUN apt-get update -qq && apt-get -y install openjdk-8-jre sbt jq postgresql-client wget zip time

ADD . /

RUN /bin/bash src/main/script/run.sh

# Run
FROM openjdk:8-jre

WORKDIR /

COPY --from=builder /indexDir /indexDir
COPY --from=builder /gnaf-search/target/scala-2.11/gnaf-search_2.11-1.1-SNAPSHOT-one-jar.jar /gnaf-search/target/scala-2.11/gnaf-search_2.11-1.1-SNAPSHOT-one-jar.jar

EXPOSE 9040

CMD ["java", "-jar", "/gnaf-search/target/scala-2.11/gnaf-search_2.11-1.1-SNAPSHOT-one-jar.jar"]
