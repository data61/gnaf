# gnaf-service

## Introduction
This project provides a [Scala](http://scala-lang.org/) [RESTful](https://en.wikipedia.org/wiki/Representational_state_transfer) web service providing access to the G-NAF database.

This is a stand-alone webapp and does not run in a servlet container.

## Configuration

Configuration is in [application.conf](src/main/resources/application.conf) and most settings can be overriden with environment variables.

## Running

    nohup java -jar target/scala-2.11/gnaf-service_2.11-0.1-SNAPSHOT-one-jar.jar >& gnaf-service.log &

## Usage

	# use this in swagger-ui
	curl 'http://localhost:9000/api-docs/swagger.json
	
	# get geocode types and descriptions
	curl 'http://localhost:9000/gnaf/geocodeType'
	
	# get type of address e.g. RURAL, often missing, for an addressDetailPid
	curl 'http://localhost:9000/gnaf/addressType/GANSW716635201'
	
	# get all geocodes for an addressDetailPid, almost always 1, sometimes 2
	curl 'http://localhost:9000/gnaf/addressGeocode/GASA_414912543'
