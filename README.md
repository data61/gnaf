# gnaf

## Introduction
This project provides:

1. gnaf-createdb: scripts to load the [G-NAF data set](http://www.data.gov.au/dataset/geocoded-national-address-file-g-naf) into a relational database.
2. gnaf-common: database and utility code shared by 3 & 4.
3. gnaf-indexer: a [Scala](http://scala-lang.org/) program to query the database to produce JSON and scripts to load this into [Elasticsearch](https://www.elastic.co/).
4. gnaf-service: a [Scala](http://scala-lang.org/) [RESTful](https://en.wikipedia.org/wiki/Representational_state_transfer) web service providing access to the database.
5. gnaf-ui: static files providing a demonstration web user interface using the Elasticsearch index and the gnaf-service.

The top level directory contains no code, but contains the [sbt](http://www.scala-sbt.org/) build for 2, 3 & 4 (no build is required for 1 & 5).

## Install Tools

To run the Scala code install:
- a JRE e.g. from openjdk-8 (version 8 or higher is required by some dependencies);
- the build tool [sbt](http://www.scala-sbt.org/).

To develop [Scala](http://scala-lang.org/) code install:
- the above items (you may prefer to install the full JDK instead of just the JRE but I think the JRE is sufficient);
- the [Scala IDE](http://scala-ide.org/download/current.html).

### Build

Automatic builds are available at: https://t3as-jenkins.it.csiro.au/job/gnaf-master/ (only within the CSIRO network).

The command:

    sbt clean publish-local one-jar dumpLicenseReport

from the project's top level directory cleans out previous build products, builds and deploys artifacts to the local ivy repository (`~/.ivy2`),
creates one-jar files (which include all dependencies) for stand-alone executables and creates license reports on dependencies.

The local ivy repository is the mechanism used to include gnaf-common in the one-jars for gnaf-indexer and gnaf-service.

### Run

This section provides a very brief summary of how to run the project. Detailed information is available in the sub-project README.md files.

	cd gnaf-createdb
	# create SQL load script
	script/createGnafDb.sh
	# run h2 with postgres protocol
	java -Xmx3G -jar ~/.ivy2/cache/com.h2database/h2/jars/h2-1.4.191.jar -web -pg &
	# create and load database (takes about 90 minutes with a SSD)
	cat data/createGnafDb.sql | psql --host=localhost --port=5435 --username=gnaf --dbname=~/gnaf
	Password for user gnaf: gnaf
	# kill h2
	kill %1
	
	cd ../gnaf-indexer
	# start Elasticsearch - see README.md (the one under gnaf-indexer) and Elasticsearch documentation
	# run indexer (which uses the database in embedded mode so will fail if the above h2 process is still running; requires jq)
	src/main/script/loadElasticsearch.sh
	
	cd ../gnaf-service
	# start gnaf database web service
	java -jar target/scala-2.11/gnaf-service_2.11-0.1-SNAPSHOT-one-jar.jar &> gnaf-service.log &
	
Test with the demonstration web user interface by opening the file `gnaf-ui/html/index.html` in a recent version of Chrome, Firefox or Edge.
	

### Develop With Eclipse

The command:

    sbt update-classifiers eclipse

uses the [sbteclipse](https://github.com/typesafehub/sbteclipse/wiki/Using-sbteclipse) plugin to create the .project and .classpath files required by Eclipse (with source attachments for dependencies).

## Software License

This software is released under the CSIRO BSD license - see `Licence.txt`.
Each of the sub-projects 2 - 4 lists its dependencies and their licenses in `3rd-party-licenses.html`.

## Data License

Incorporates or developed using G-NAF ©PSMA Australia Limited licensed by the Commonwealth of Australia under the
[Open Geo-coded National Address File (G-NAF) End User Licence Agreement](http://data.gov.au/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc/resource/09f74802-08b1-4214-a6ea-3591b2753d30/download/20160226---EULA---Open-G-NAF.pdf).

