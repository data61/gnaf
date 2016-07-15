# gnaf-contrib

## Introduction
This project provides a [Scala](http://scala-lang.org/) [RESTful](https://en.wikipedia.org/wiki/Representational_state_transfer) web service providing access to the
gnafContrib database of user supplied geocodes.

This is a stand-alone webapp and does not run in a servlet container.
On startup the database schema is created if it doesn't already exist.

## Configuration

Configuration is in [application.conf](src/main/resources/application.conf) and most settings can be overriden with environment variables.

## Running

    nohup java -jar target/scala-2.11/gnaf-contrib_2.11-0.1-SNAPSHOT-one-jar.jar >& gnaf-contrib.log &

## Usage

	# use this in swagger-ui
	curl 'http://localhost:9010/api-docs/swagger.json
	
	#  add contributed geocode for an addressSite
	curl -XPOST 'http://gnaf.it.csiro.au:9010/contrib/' -H 'Content-Type:application/json' -d '{
	  "contribStatus":"Submitted","addressSitePid":"712279621","geocodeTypeCode":"EM",
	  "longitude":149.1213974,"latitude":-35.280994199999995,"dateCreated":0,"version":0
	}'
	
	# list contributed geocodes for an addressSite
	curl 'http://gnaf.it.csiro.au:9010/contrib/712279621'
	
	# there are also delete and update methods

### Generate Slick bindings

The slick bindings can be written by hand, but its quicker to generate the bindings from a manually created database table: 

Create and connect to a new database with dburl `jdbc:h2:file:~/gnafContrib`, username `gnaf` and password `gnaf`.
Create a table from which the bindings will be generated:

	CREATE TABLE ADDRESS_SITE_GEOCODE (
	  id long IDENTITY,                      -- auto-inc primary key
	  contrib_status varchar(15) NOT NULL,   -- ‘SUBMITTED’|‘PUBLISHED’
	  address_site_geocode_pid varchar(15),  -- set to correct a gnaf geocode, null to add a new one
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

