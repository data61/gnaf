# gnaf

## Introduction
This project:

- loads the [G-NAF data set](http://www.data.gov.au/dataset/geocoded-national-address-file-g-naf) into a relational database and search engine;
- provides JSON web services to access the database and search engine; and
- provides a demonstration web user interface using the web services.

Users of `gnaf-search` should note the [suggested preprocessing](gnaf-lucene/README.md#suggested-preprocessing-for-client-applications) for
query strings.

## Project Structure
These sub-directories contain sub-projects:

1. gnaf-util: common code
2. gnaf-db: scripts to load the [G-NAF data set](http://www.data.gov.au/dataset/geocoded-national-address-file-g-naf) into a relational database
and [Slick](http://slick.typesafe.com/) "Functional Relational Mapping" bindings for the database.
The README.md discusses the H2 database and G-NAF data.
3. gnaf-extractor: queries the database to produce JSON address data
4. gnaf-lucene: common code for indexing and searching G-NAF with [Lucene](https://lucene.apache.org/).
The README.md discusses the search techniques used.
5. gnaf-indexer: loads JSON address data into a [Lucene](https://lucene.apache.org/) index
6. gnaf-search: JSON web service to search the [Lucene](https://lucene.apache.org/) index
7. gnaf-test: queries the database to produce test address data with many variations, scripts to perform bulk lookups of the test data and evaluate results
8. gnaf-db-service: JSON web service providing access to the G-NAF database
9. gnaf-contrib: a JSON web service providing access to the gnafContrib database of user supplied geocodes
10. gnaf-ui: static files providing a demonstration web user interface using gnaf-search, gnad-db-service and gnaf-contrib.

Nature of Sub-projects:

- 1, 2 & 4 produce a jar file of library code used by other sub-projects
- 3, 5 & 7 produce command line programs packaged as a [onejar](https://github.com/sbt/sbt-onejar).
This is a jar file containing all dependencies and run simply with: `java -jar {filename.jar}`
- 6, 8 & 9 produce JSON web services also packaged as a [onejar](https://github.com/sbt/sbt-onejar).
These are run as above (not in a servlet container). They produce [Swagger](http://swagger.io/) API documentation at `/api-docs/swagger.json`.

The top level directory provides:
- the [sbt](http://www.scala-sbt.org/) build for the [Scala](http://scala-lang.org/) code in projects 1-9 (no build is required for 10); and
- [src/main/script/run.sh](src/main/script/run.sh) to run everything, but first:
  - take a look as its intended as executable documentation and you may not wish to run it all each time
  - install tools

## Install Tools

To run the Scala code install:
- a JRE e.g. from openjdk-8 (version 8 or higher is required by some dependencies);
- the build tool [sbt](http://www.scala-sbt.org/).

To develop [Scala](http://scala-lang.org/) code install:
- the above items (you may prefer to install the full JDK instead of just the JRE but I think the JRE is sufficient);
- the [Scala IDE](http://scala-ide.org/download/current.html).

### Dependencies

- scripts assume a *nix environment
- [gnaf-db/src/main/script/createGnafDb.sh](gnaf-db/src/main/script/createGnafDb.sh) requires [jq](https://stedolan.github.io/jq/)
- [src/main/script/run.sh](src/main/script/run.sh) requires:
  - `jq` (because it runs `createGnafDb.sh`)
  - the Postgres client `psql` to load the database (see [gnaf-db](gnaf-db) for an alternative method using the h2 client);
  - `node` and `npm` to run [gnaf-test](gnaf-test) (see its README).
- the `/version` endpoint provided by `gnaf-search` reports the software and data version, but relies on a file created by `createGnafDb.sh`
  being available when gnaf-search is built. `run.sh` does things in the right order for this to work.

## Running and Usage

See [src/main/script/run.sh](src/main/script/run.sh).

## Build

Automatic builds are available at: https://t3as-jenkins.it.csiro.au/ (only within the CSIRO network).

The command:

    sbt clean test one-jar dumpLicenseReport

from the project's top level directory cleans out previous build products, runs unit tests,
builds one-jar files and creates license reports on dependencies.

## Develop With Eclipse

The command:

    sbt update-classifiers eclipse

uses the [sbteclipse](https://github.com/typesafehub/sbteclipse/wiki/Using-sbteclipse) plugin to create the .project and .classpath files required by Eclipse (with source attachments for dependencies).

## Software License

This software is released under the CSIRO BSD license - see `Licence.txt`.
Each of the sub-projects lists its dependencies and their licenses in `3rd-party-licenses.html`.

## Data License

Incorporates or developed using G-NAF ©PSMA Australia Limited licensed by the Commonwealth of Australia under the
[Open Geo-coded National Address File (G-NAF) End User Licence Agreement](http://data.gov.au/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc/resource/09f74802-08b1-4214-a6ea-3591b2753d30/download/20160226---EULA---Open-G-NAF.pdf).

