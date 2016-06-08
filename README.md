# gnaf

## Introduction
This project provides:

1. gnaf-createdb: scripts to load the [G-NAF data set](http://www.data.gov.au/dataset/geocoded-national-address-file-g-naf) into a relational database.
2. gnaf-common: database and utility code shared by 3 & 4.
3. gnaf-indexer: a [Scala](http://scala-lang.org/) program to query the database to produce JSON and scripts to load this into [Elasticsearch](https://www.elastic.co/).
4. gnaf-service: a [Scala](http://scala-lang.org/) [RESTful](https://en.wikipedia.org/wiki/Representational_state_transfer) web service providing access to the database.
5. gnaf-ui: static files providing a demonstration web user interface using the Elasticsearch index and the gnaf-service.

The top level directory contains no code, but contains the [sbt](http://www.scala-sbt.org/) build for 2, 3 & 4.

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

    sbt clean package oneJar dumpLicenseReport

from the project's top level directory cleans out previous build products, creates jar files of compiled code,
creates oneJar files for stand-alone executables (which include all dependencies) and creates license reports on dependencies.

### Develop With Eclipse

The command:

    sbt update-classifiers eclipse

uses the [sbteclipse](https://github.com/typesafehub/sbteclipse/wiki/Using-sbteclipse) plugin to create the .project and .classpath files required by Eclipse (with source attachments for dependencies).

## Data License

Incorporates or developed using G-NAF ©PSMA Australia Limited licensed by the Commonwealth of Australia under the
[http://data.gov.au/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc/resource/09f74802-08b1-4214-a6ea-3591b2753d30/download/20160226---EULA---Open-G-NAF.pdf](Open Geo-coded National Address File (G-NAF) End User Licence Agreement).

