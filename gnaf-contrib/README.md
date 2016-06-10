# gnaf-contrib

## Introduction

[Slick](http://slick.typesafe.com/) provides "Functional Relational Mapping for Scala".
This project provides a Slick mapping for the GNAF Contrib database.
The mappings are not tied to any particular relational database.

## Generate Slick bindings

Create and connect to a new database with dburl `jdbc:h2:file:~/gnafContrib`, username `gnaf` and password `gnaf`.
Create a table from which the bindings will be generated:

	CREATE TABLE ADDRESS_SITE_GEOCODE (
	  id long IDENTITY,                      -- auto-inc primary key
	  contrib_status varchar(15) NOT NULL,   -- ‘SUBMITTED’|‘PUBLISHED’
	  address_site_geocode_pid varchar(15),  -- set for correction, null for addition
	  date_created date NOT NULL,
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
        Array("slick.driver.H2Driver", "org.h2.Driver", "jdbc:h2:file:~/gnafContrib", "generated", "au.csiro.data61.gnaf.contrib.db", "gnaf", gnaf")
    )

This generates code in: `generated/au/csiro/data61/gnaf/contrib/db/Tables.scala`.
The source file `src/main/scala/au/csiro/data61/gnaf/contrib/db/ContribTables.scala` is a very minor modification of this generated code.

