# Build
FROM ubuntu:16.04 as builder

WORKDIR /

RUN apt-get update

RUN apt-get -y install apt-transport-https

RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
RUN echo "deb http://apt.postgresql.org/pub/repos/apt/ xenial-pgdg main"
RUN apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys B97B0AFCAA1A47F044F244A07FCC7D46ACCC4CF8
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823

RUN apt-get update

RUN apt-get -y install openjdk-8-jre sbt jq postgresql wget curl zip time

ADD . /

RUN /bin/bash src/main/script/run.sh

# Run
FROM openjdk:8-jre

WORKDIR /

COPY --from=builder /indexDir /indexDir
COPY --from=builder /gnaf-search/target/scala-2.11/gnaf-search_2.11-1.1-SNAPSHOT-one-jar.jar /gnaf-search/target/scala-2.11/gnaf-search_2.11-1.1-SNAPSHOT-one-jar.jar

EXPOSE 9040

CMD ["java", "-jar", "/gnaf-search/target/scala-2.11/gnaf-search_2.11-1.1-SNAPSHOT-one-jar.jar"]
