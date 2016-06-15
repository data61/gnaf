# gnaf-contrib
## Introduction
This project provides a [Scala](http://scala-lang.org/) [RESTful](https://en.wikipedia.org/wiki/Representational_state_transfer) web service providing access to the
gnafContrib database of user supplied geocodes.

This is a stand-alone webapp and does not run in a servlet container.

## Configuration

Configuration is in [application.conf](src/main/resources/application.conf) and most settings can be overriden with environment variables.

## Running

    nohup java -jar target/scala-2.11/gnaf-contrib_2.11-0.1-SNAPSHOT-one-jar.jar >& gnaf-contrib.log &

## Usage
The service supports CRUD operations on user contributed geocodes. Examples:

	# list contributed geocodes for an addressSitePid
	curl 'http://gnaf.it.csiro.au:9010/contrib/712279621'
	# create a contributed geocode
	curl -XPOST -H 'Content-Type:application/json; charset=UTF-8' http://gnaf.it.csiro.au:9010/contrib/ -d '{"contribStatus":"Submitted","addressSitePid":"712279621","geocodeTypeCode":"GCP","longitude":149.1213974,"latitude":-35.2809942,"dateCreated":0,"version":0}'
	# delete a contributed geocode (doesn't work with curl)
	curl -XDELETE -H 'Content-Type:application/json; charset=UTF-8' http://gnaf.it.csiro.au:9010/contrib/ -d '{"id":18,"version":1}'
	# update a contributed geocode
	curl -XPUT -H 'Content-Type:application/json; charset=UTF-8' http://gnaf.it.csiro.au:9010/contrib/ -d '{"contribStatus":"FredWasHere","addressSitePid":"712279621","geocodeTypeCode":"GCP","longitude":149.1213974,"latitude":-35.2809942,"dateCreated":0,"id":19,"version":1}'
	
## To Do

Have the service create the database table on start up if it doesn't already exist.

Review RESTfullness of API.

## Database Mapping

[Slick](http://slick.typesafe.com/) provides "Functional Relational Mapping for Scala".
This project provides a Slick mapping for the GNAF Contrib database.
The mappings are not tied to any particular relational database.

### Generate Slick bindings

Create and connect to a new database with dburl `jdbc:h2:file:~/gnafContrib`, username `gnaf` and password `gnaf`.
Create a table from which the bindings will be generated:

	CREATE TABLE ADDRESS_SITE_GEOCODE (
	  id long IDENTITY,                      -- auto-inc primary key
	  contrib_status varchar(15) NOT NULL,   -- ‘SUBMITTED’|‘PUBLISHED’
	  address_site_geocode_pid varchar(15),  -- set for correction, null for addition
	  date_created date NOT NULL,
	  version int NOT NULL,                  -- optimistic locking row version
	  address_site_pid varchar(15) NOT NULL,
	  geocode_type_code varchar(4) NOT NULL,
	  longitude numeric(11,8) NOT NULL,
	  latitude numeric(10,8) NOT NULL
	);

Disconnect the SQL client from the database then, from the top level gnaf directory:

    sbt
    > project gnafContrib
    > console
    slick.codegen.SourceCodeGenerator.main(
        Array("slick.driver.H2Driver", "org.h2.Driver", "jdbc:h2:file:~/gnafContrib", "generated", "au.csiro.data61.gnaf.contrib.db", "gnaf", "gnaf")
    )

This generates code in: `generated/au/csiro/data61/gnaf/contrib/db/Tables.scala`.
The source file `src/main/scala/au/csiro/data61/gnaf/contrib/db/ContribTables.scala` is a very minor modification of this generated code.

