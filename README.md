# gnaf
## Introduction
This project provides:

1. A script to load the [G-NAF data set](http://www.data.gov.au/dataset/geocoded-national-address-file-g-naf) into a relational database.
2. A [Scala](http://scala-lang.org/) program to query the database to produce JSON to load into a search engine.
3. A script to run the Scala program, transform the output to suit [Elasticsearch](https://www.elastic.co/)'s 'bulk' API and load the data.
4. A web page to query the Elasticsearch index.

## Install Tools

To run the Scala code install:
- a JRE e.g. from openjdk-7 or 8;
- the build tool [sbt](http://www.scala-sbt.org/).

To develop [Scala](http://scala-lang.org/) code install:
- the above items (you may prefer to install the full JDK instead of just the JRE but I think the JRE is sufficient);
- the [Scala IDE](http://scala-ide.org/download/current.html).

Run `sbt update-classifiers` to download dependencies including the H2 database engine used in the next section.

## H2 database
H2 provides single file, zero-admin databases.

### Result Set Spooling
If an [H2](http://www.h2database.com/) result set contains more than
[MAX_MEMORY_ROWS](http://www.h2database.com/html/grammar.html?highlight=max_memory_rows&search=MAX_MEMORY_ROWS#set_max_memory_rows),
it is spooled to disk before the first row is provided to the client.
The default is 40000 per GB of available RAM and setting a non-default value requires database admin rights (which we prefer to avoid using).
Analysis in comments in `Main.scala` shows that we need to handle result sets up to 95,004 rows, so allocating up to 3GB of heap (with `java -Xmx3G`) should avoid spooling.

### Postgres Protocol
H2 supports the Postgres protocol, allowing access from any Postgres compatible client.

Start the [H2 server](http://www.h2database.com/html/tutorial.html#using_server) with options:

- `-web` to start the HTTP H2 Console on port 8082; and
- `-pg` to start the Postgres protocol on port 5435 (different from Postgres Server default of 5432 so as not to clash).

Options are available to change these default ports.
Upon the first connection using the Postgres protocol, H2 runs a script to create Postgres compatible system views and this requires a user with admin rights.

First connection with admin rights:

        psql --host=localhost --port=5435 --username=gnaf --dbname=~/sw/gnaf/data/gnaf

Subsequent connections may use reduced access rights:

        psql --host=localhost --port=5435 --username=READONLY --dbname=~/sw/gnaf/data/gnaf

psql cannot connect with blank username or password.

## Create Database

This section describes automation of the procedure described in the G-NAF [getting started guide](https://www.psma.com.au/sites/default/files/g-naf_-_getting_started_guide.pdf).
See also https://github.com/minus34/gnaf-loader as an alternative (which makes some modifications to the data).

### Create SQL Load Script

Running:

    src/main/script/createGnafDb.sh

- downloads the G-NAF zip file to `data/` (if not found);
- unzips to `data/unzipped/` (if not found); and
- writes SQL to create the H2 database to `data/createGnafDb.sql`.


### Start Database
The [H2](http://www.h2database.com/) database engine is started with:

    java -Xmx3G -jar ~/.ivy2/cache/com.h2database/h2/jars/h2-1.4.191.jar -web -pg

(the H2 jar file was put here by `sbt update-classifiers`, alternatively download the jar from the H2 web site and run it as above).

The database engine is stopped with `Ctrl-C` (but not yet as it's needed for the next step).

### Run SQL Load Script
The instructions in this section describe use of the H2 Console webapp, a SQL client running at: http://127.0.1.1:8082/, to run the SQL Load Script `data/createGnafDb.sql`. Alternatively a Postgres client could be used.

In the SQL client, enter JDBC URL: `jdbc:h2:file:~/sw/gnaf/data/gnaf`, User name: `gnaf` and Password: `gnaf`) and click `Connect`. If a database doesn't already exist at this location an empty database is created with the given credentials as the admin user.

Run the SQL commands either by:
- entering: `RUNSCRIPT FROM '~/sw/gnaf/data/createGnafDb.sql'` into the SQL input area (this method displays no indication of progress); or
- pasting the content of this file into the SQL input area (this method displays what is going on).

On a macbook-pro (with SSD) it takes 26 min to load the data and another 53 min to create the indexes.
The script creates a user `READONLY` with password `READONLY` that has only the `SELECT` right. This user should be used for read-only access.

### Example Queries
Find me (fast):

    SELECT SL.*, AD.*
    FROM
        STREET_LOCALITY SL
        LEFT JOIN ADDRESS_DETAIL AD ON AD.STREET_LOCALITY_PID = SL.STREET_LOCALITY_PID  
    WHERE SL.STREET_NAME = 'TYTHERLEIGH'
        AND AD.NUMBER_FIRST = 14

This is slow (45892 ms):

    SELECT * FROM ADDRESS_VIEW 
    WHERE STREET_NAME = 'TYTHERLEIGH'
    AND NUMBER_FIRST = 14

but at least this is fast:

    SELECT * FROM ADDRESS_VIEW 
    WHERE ADDRESS_DETAIL_PID = 'GAACT714928273'

Although data quality is generally very good, this shows some dodgy STREET_LOCALITY_ALIAS records:

    SELECT sl.STREET_NAME, sl.STREET_TYPE_CODE, sl.STREET_SUFFIX_CODE,
      sla.STREET_NAME, sla.STREET_TYPE_CODE , sla.STREET_SUFFIX_CODE 
    FROM STREET_LOCALITY_ALIAS sla, STREET_LOCALITY sl
    WHERE sla.STREET_LOCALITY_PID = sl.STREET_LOCALITY_PID
    AND sl.STREET_NAME = 'REED'
        
    STREET_NAME     STREET_TYPE_CODE    STREET_SUFFIX_CODE      STREET_NAME     STREET_TYPE_CODE    STREET_SUFFIX_CODE  
    REED            STREET              S                       REED STREET     SOUTH               null
    REED            STREET              N                       REED STREET     NORTH               null

Here is an example showing a cul-de-sac with some even numbered houses:

    SELECT
      SL.STREET_NAME, SL.STREET_TYPE_CODE, SL.STREET_SUFFIX_CODE, SL.STREET_LOCALITY_PID,
      SLA.STREET_NAME, SLA.STREET_TYPE_CODE, SLA.STREET_SUFFIX_CODE, SLA.ALIAS_TYPE_CODE
    FROM
      STREET_LOCALITY as SL
      left join STREET_LOCALITY_ALIAS as SLA on SL.STREET_LOCALITY_PID = SLA.STREET_LOCALITY_PID
    WHERE SL.STREET_NAME = 'OFFICER'

    STREET_NAME  STREET_TYPE_CODE  STREET_SUFFIX_CODE  STREET_LOCALITY_PID  STREET_NAME  STREET_TYPE_CODE  STREET_SUFFIX_CODE  ALIAS_TYPE_CODE  
    OFFICER     CRESCENT        null    ACT3379850      null    null    null    null
    OFFICER     CRESCENT        null    ACT253      null        null    null    null
    OFFICER     PLACE       null        ACT254   OFFICER CRESCENT       null    SYN

STREET_LOCALITY_PID `ACT3379850` not used in any ADDRESS_DETAIL. `ACT253` is used for all numbers on this street except `ACT254` used for even nos from [80 – 98].
So for most numbers only CRESCENT is acceptable, for these even nos 'PLACE' is correct but 'CRESCENT' is also acceptable.

Looking at cases where the alias has a different STREET_NAME (27119 cases):

- the vast majority appear to be spelling variants with an edit distance of 1 - exception FLAGSTONE -> WHISKEY BAY
- our queries will match with an edit distance of 2 (after an initial 2 character match), so these will mostly match
  however O'Farrel -> OFarrel won't match (tried prefix_length = 1 but that made "18 London circuit" match some other number first)
- in some cases the number of tokens is different e.g. DE CHAIR -> DECHAIR, TWELVETREES -> TWELVE TREES,
  however the combination of the shingle filter (1 - 3-grams) and edit distance matching will make these match
- there are aliases for SAINT X -> ST X (237), ST X -> SAINT X (26), MOUNT X -> MT X (577) and MT X -> MOUNT X (110)

Currently the suggesters (auto-complete) do not search street and locality aliases (see To Do section), but the searches do.

### Code/name Lookup Tables

For all tables in this section the values in the `DESCRIPTION` and `NAME` columns are the same.

For the tables `FLAT_TYPE_AUT`, `LEVEL_TYPE_AUT` and `STREET_SUFFIX_AUT`:
- `CODE` is a short abbreviation containing only letters;
- `NAME` is the full name/description that may contain spaces.

However for `STREET_TYPE_AUT` these roles are reversed:
- `CODE` is the full name/description: alphabetic only except for "CUL-DE-SAC" and "RIGHT OF WAY";
- `NAME` is a short abbreviation containing only letters.

One row ( `CODE` = AWLK, `NAME` = AIRWALK ) breaks this rule, with the abbreviation stored in `CODE`. 

The following sections show sample rows from these tables and the number of rows.

#### FLAT_TYPE_AUT

    SELECT * FROM FLAT_TYPE_AUT
    CODE    NAME                      DESCRIPTION 
    ATM     AUTOMATED TELLER MACHINE  AUTOMATED TELLER MACHINE
    APT     APARTMENT                 APARTMENT
    FLAT    FLAT                      FLAT
    SE      SUITE                     SUITE
    STU     STUDIO                    STUDIO
    UNIT    UNIT                      UNIT
    ...
    
53 rows, many rather obscure.

#### LEVEL_TYPE_AUT
    
        SELECT * FROM LEVEL_TYPE_AUT
        CODE    NAME            DESCRIPTION  
        B       BASEMENT        BASEMENT
        FL      FLOOR           FLOOR
        G       GROUND          GROUND
        L       LEVEL           LEVEL
        LB      LOBBY           LOBBY
        LG      LOWER GROUND FLOOR      LOWER GROUND FLOOR
        M       MEZZANINE       MEZZANINE
        ...
        
15 rows.

#### STREET_SUFFIX_AUT

        SELECT * FROM STREET_SUFFIX_AUT
        CODE    NAME    DESCRIPTION  
        CN      CENTRAL CENTRAL
        DE      DEVIATION       DEVIATION
        NE      NORTH EAST      NORTH EAST
        EX      EXTENSION       EXTENSION
        LR      LOWER   LOWER
        ...
        
19 rows.

#### STREET_TYPE_AUT
       
        SELECT * FROM STREET_TYPE_AUT
        CODE    NAME    DESCRIPTION  
        HIKE    HIKE    HIKE
        AWLK    AIRWALK AIRWALK
        FLATS   FLTS    FLTS
        BOARDWALK       BWLK    BWLK
        BOULEVARD       BVD     BVD
        BOULEVARDE      BVDE    BVDE
        CLOSE   CL      CL
        COURT   CT      CT
        CUL-DE-SAC      CSAC    CSAC
        DRIVE   DR      DR
        STREET  ST      ST
        ...
        
265 rows, many rather obscure. In contrast to all the previous tables, CODE is the full text which can contain spaces and NAME is the short abbreviation.

### ADRESS_DETAIL Rows
- non null numberLast: 1,121,843 out of 14,126,043
- flatNumber prefix length 2, 14,099,611 nulls; suffix length 2, 4,017,287 nulls, both with lots of letter and number combinations
- level prefix length 2, 14,123,096 nulls; suffix length 2, 14,125,593 nulls
- numberFirst prefix length 3, 14,122,945 nulls; suffix length 2, 13,493,586 nulls
- numberLast prefix length 3; suffix length 2

    
## Build and Development Set Up
The script described in the "Elasticsearch | Create Index" section runs the build, so most readers can safely skip this section.

### Build

Automatic builds are provided at: not yet

The command:

    sbt clean test package oneJar dumpLicenseReport

from the project's top level directory cleans out previous build products, runs tests, creates a jar file of the compiled code,
creates a oneJar file which is a stand-alone executable including all dependencies and creates a report on the licenses of dependencies.

### Generate Slick bindings
[Slick](http://slick.typesafe.com/) provides "Functional Relational Mapping for Scala".
To generate Slick mappings for the database (first disconnect any other clients):

    mkdir -p generated
    sbt
    > console
    slick.codegen.SourceCodeGenerator.main(
        Array("slick.driver.H2Driver", "org.h2.Driver", "jdbc:h2:file:~/sw/gnaf/data/gnaf", "generated", "au.com.data61.gnaf.db")
    )

This generates code in the `generated/au/com/data61/gnaf/db` directory.
At this stage its not clear whether the mapping will:

1. need hand tweaks (i.e. once off generation then its part of the source code); or
2. not need hand tweaks and should be generated by the build and is not part of our source code (better if schema changes much/often).

For now we'll opt for 1 and manually move this into `src/main/scala`. So far it has not needed tweaking and there has been an unsuccessful attempt at using `sbt-slick-codegen` for option 2 (see comments in `project/plugins.sbt`).

### Develop With Eclipse

The command:

    sbt update-classifiers eclipse

uses the [sbteclipse](https://github.com/typesafehub/sbteclipse/wiki/Using-sbteclipse) plugin to create the .project and .classpath files required by Eclipse (with source attachments for dependencies).

## Run
The script described in the "Elasticsearch | Create Index" section runs the code, so most readers can safely skip this section.

The command:

    java -jar target/scala-2.11/bpcsimilarity_2.11-0.1-SNAPSHOT-one-jar.jar --help
    
shows help on command line options, which should be sufficient to run the code.

## Elasticsearch
### Configuration
Elasticsearch configuration is in `config/elasticsearch.yml`.

To prevent the node joining some other Elasticsearch cluster change the cluster name from the default 'elasticsearch':

    cluster.name: gnaf
    
Access by the client apps requires [CORS](https://en.wikipedia.org/wiki/Cross-origin_resource_sharing) to be configured:

    http.cors.enabled: true
    http.cors.allow-origin: "*"
        
It may be necessary to increase the default heap size for Elasticsearch (at least during indexing). The [docs](https://www.elastic.co/guide/en/elasticsearch/guide/current/heap-sizing.html) suggest allocating 50% of physical memory (up to no more than a bit less than 32G). If Elasticsearch is installed from a deb/rpm package set `ES_HEAP_SIZE=2g` in the service startup script `/etc/init.d/elasticsearch`; if installed from a zip/tarball export this shell variable before running the startup script `bin/elasticsearch`.

Restart Elasticsearch for the changes to take effect.

### Create Index

Running:

    src/main/script/loadElasticSearch.sh

builds and runs the Scala program, transforms the output to suit Elasticsearch's 'bulk' API and loads the data to create an Elasticsearch index. Dependencies:

- the JSON transformation tool [jq](https://stedolan.github.io/jq/);
- Elasticsearch running on http://localhost:9200 (that is its default port).

### Example Queries

Get the schema/mapping (created by above script loading `src/main/resouces/gnafMapping.json`):

	$ curl -XGET 'localhost:9200/gnaf/?pretty'
	
Search for an exact match, retrieve at most 5 results:

    $ curl -XPOST 'localhost:9200/gnaf/_search?pretty' -d '
    {
      "query": { "match": { "street.name": "CURRONG" } },
      "size": 5
    }' 

Search for a fuzzy match against all fields:
        
    $ curl -XPOST 'localhost:9200/gnaf/_search?pretty' -d '
    {
      "query": { "match": { "_all": { "query": "CURRONGT",  "fuzziness": 1, "prefix_length": 2 } } },
      "size": 5
    }' 


Search for a fuzzy match against the `d61Address` field which uses a shingle filter (n-grams n = 1, 2, 3) set up in the schema/mapping:

    curl -XPOST 'localhost:9200/gnaf/_search?pretty' -d '
    {
        "query": {
            "match": {
                "d61Address": {
                    "query": "7 LONDON CIRCUIT, CITY ACT 2601",
                    "fuzziness": 2,
                    "prefix_length": 2
                }
            }
        },
        "size": 5
    }'

Search for addresses within 200m of a location:

    curl -XPOST 'localhost:9200/gnaf/_search?pretty' -d '
    {
        "query": {
            "bool" : {
                "must" : {
                    "match_all" : {}
                },
                "filter" : {
                    "geo_distance" : {
                        "distance" : "200m",
                        "location" : {
                            "lat" : -35.28150121,
                            "lon" : 149.12512965
                        }
                    }
                }
            }
        },
        size: 5
    }'

## Client Apps

### Web Page
The web page `src/main/webapp/index.html` provides a user interface to query Elasticsearch.

#### Data Entry
- the entire address may be entered as free text in a single field; or
- the address can be entered into separate fields for street, locality, postcode, state etc.
This method provides autocompletion on the street field which also sets the locality, postcode, state; thus ensuring valid data entry.

#### Search Strategies
For free text address entry a fuzzy search is performed on the `d61Address` field as shown in the examples above.
The shingle (n-gram) filter strongly rewards words appearing in the correct order whilst still matching terms out-of-order.

When the address is entered into separate fields it is parsed into finer grained GNAF fields for a very specific search.

See `src/main/webapp/query.js` for the actual query generation.

### Command Line
The [node.js](https://nodejs.org/) command line client was created for bulk lookup of 1.1M addresses from the DIBP Mail project. It requires node's `request` module, which is installed with:

    npm init
    npm install request
    
Run the [node.js](https://nodejs.org/) command line client with:

    node src/main/webapp/main.js

It reads lines from stdin purporting to be an address and writes the top Elasticsearch match as JSON to stdout.

Example usage:

This didn't work:

    zcat ../dibpMail/data.tsv.gz | cut -f29-30,32,33,53  | sed -e '1d' -e 's/\t/~/g' | node src/main/webapp/main.js > dibpMailAddresses
    Error { [Error: connect EADDRNOTAVAIL 127.0.0.1:9200 - Local (127.0.0.1:0)]
    
I think the async node request module is firing off too many concurrent requests to Elasticsearch. This works OK, but I think firing up node for each query is slowing it down:

    zcat ../dibpMail/data.tsv.gz | cut -f29-30,32,33,53  | sed -e '1d' -e 's/\t/~/g' | while read a; do echo $a | node src/main/webapp/main.js; done > dibpMailAddresses

We could try splitting the input into chunks that are small enough to process completely asynchronously:

    zcat ../dibpMail/data.tsv.gz | cut -f29-30,32,33,53  | sed -e '1d' -e 's/\t/~/g' | split -l40 -a6 - chunk-
    for f in chunk-*; do node src/main/webapp/main.js < $f; done > dibpMailAddresses


## To Do

Add Code/Name from the _AUT tables as synonyms (e.g. so ST will match STREET) to the phrase suggester.
The current indexed term is the full name (which may contain spaces), so we need to add the abbreviation (which does not contain spaces).
A difference in spaces alters the number of tokens and all the following term positions resulting in problems with phrase search.
See https://www.elastic.co/guide/en/elasticsearch/guide/current/multi-word-synonyms.html, which suggest using "Simple Contraction".
However we're using shingles/ngrams rather than phrase search, so do we have the same problem? Yes I think so.
We should do contraction to the single term abreviation.
Possible negative consequences? Synonyms create the risk of spurious matches. The tables contain some unused entries (e.g. the STREET_TYPE_AUT (AWLK, AIRWALK)) and many rarely used entries; using them all as synonyms increases the risk. e.g. ATM has small edit distance from ATMA, ATKA, ATEA (street names), so contracting "AUTOMATIC TELLER MACHINE" to "ATM" could result in these street names matching AUTOMATIC TELLER MACHINEs.
Perhaps we need to be quite selective in the use of synonyms.

Add street (locality) aliases and locality aliases to the phrase suggester.
Don't think we can use contraction here because one is not the contraction of the other, they are both multi-term.
Simply appending the alias terms will probably be sufficient.

Other synonyms: "St" for "Saint", "Mt" for "Mount"?
The "Example Queries" section shows that this would be handled by including street (locality) aliases.


The phrase suggester I think is performing better than the main query. Replace the latter with the query from the former.

At some cost in terms of speed, we could prioritize primary over secondary addresses and principle over alias addresses (already done in the free text search). But maybe the default higher weigh given to shorter docs is already enough?

https://www.elastic.co/guide/en/elasticsearch/guide/current/_dealing_with_null_values.html
The mapping can have a value to substitute for null values, so we can do away with that from the Scala code.

The free text query (with heuristics) takes ~0.07 sec (without heuristics it takes ~1.1 sec):

    "{"query":{"bool":{"should":[{"constant_score":{"filter":{"term":{"primarySecondary":"0"}},"boost":0.8}},{"constant_score":{"filter":{"term":{"primarySecondary":"P"}},"boost":1}},{"constant_score":{"filter":{"term":{"aliasPrincipal":"P"}},"boost":1}},{"match":{"d61NullStr":"D61_NULL"}},{"match":{"d61NullInt":"-1"}},{"term":{"stateAbbreviation":"ACT"}},{"term":{"postcode":"2601"}},{"term":{"numberFirst.number":"7"}},{"multi_match":{"query":"7","fields":["level.number^0.2","flat.number^0.4","numberFirst.number^0.5","numberLast.number^0.3","postcode^0.5"],"type":"most_fields"}},{"match":{"_all":{"query":" LONDON CCT, ","fuzziness":1,"prefix_length":2}}}],"minimum_should_match":"75%"}},"size":10}"

The equivalent fields query takes ~0.03 sec:

    "{"query":{"bool":{"should":[{"term":{"level.number":-1}},{"term":{"flat.number":-1}},{"term":{"numberFirst.prefix":"D61_NULL"}},{"term":{"numberFirst.number":"7"}},{"term":{"numberFirst.suffix":"D61_NULL"}},{"match":{"d61Street":{"query":"LONDON CCT","fuzziness":1,"prefix_length":2}}},{"term":{"postcode":"2601"}},{"term":{"stateAbbreviation":"ACT"}}],"minimum_should_match":"75%"}},"size":10}"

## Data License

Incorporates or developed using G-NAF ©PSMA Australia Limited licensed by the Commonwealth of Australia under the
[http://data.gov.au/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc/resource/09f74802-08b1-4214-a6ea-3591b2753d30/download/20160226---EULA---Open-G-NAF.pdf](Open Geo-coded National Address File (G-NAF) End User Licence Agreement).

