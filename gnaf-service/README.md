# gnaf-service
## Introduction
This project provides a [Scala](http://scala-lang.org/) [RESTful](https://en.wikipedia.org/wiki/Representational_state_transfer) web service providing access to the G-NAF database.

This is a stand-alone webapp and does not run in a servlet container.

## Configuration

Configuration is in [application.conf](src/main/resources/application.conf) and most settings can be overriden with environment variables.

## Running

    nohup java -jar target/scala-2.11/gnaf-service_2.11-0.1-SNAPSHOT-one-jar.jar >& gnaf-service.log &

## Usage
The service supports GET requests on two routes, with the last portion of the URL being an ADDRESS_DETAIL_PID:

	curl 'http://localhost:9000/addressGeocode/GASA_414912543'
	curl 'http://localhost:9000/addressType/GANSW716635201'