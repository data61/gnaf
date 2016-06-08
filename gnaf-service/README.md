# gnaf-service
## Introduction
This project provides a [Scala](http://scala-lang.org/) [RESTful](https://en.wikipedia.org/wiki/Representational_state_transfer) web service providing access to the database.

This is a stand-alone webapp and does not run in a servlet container.

## Configuration

Configuration is in `src/main/resources/application.conf` and most settings can be overriden with environment variables (we could also add command line options).

## Running

    nohup java -jar target/scala-2.11/gnaf-service_2.11-0.1-SNAPSHOT-one-jar.jar >& gnaf-service.log &

## Data License

Incorporates or developed using G-NAF ©PSMA Australia Limited licensed by the Commonwealth of Australia under the
[http://data.gov.au/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc/resource/09f74802-08b1-4214-a6ea-3591b2753d30/download/20160226---EULA---Open-G-NAF.pdf](Open Geo-coded National Address File (G-NAF) End User Licence Agreement).

