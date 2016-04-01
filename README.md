# gnaf
Components of this project:

1. A script loads the [G-NAF data set](http://www.data.gov.au/dataset/geocoded-national-address-file-g-naf) into a relational database.
2. A [Scala](http://scala-lang.org/) program queries the database to produce JSON to load into a search engine.
3. A script runs the Scala program, transforms the output to suit [Elasticsearch](https://www.elastic.co/)'s 'bulk' API and loads the data.
4. A web page queries the Elasticsearch index.

## Install Tools

To run the Scala code install:
- a JRE e.g. from openjdk-7 or 8;
- the build tool [sbt](http://www.scala-sbt.org/).

To develop [Scala](http://scala-lang.org/) code install:
- the above items (you may prefer to install the full JDK instead of just the JRE but I think the JRE is sufficient);
- the [Scala IDE](http://scala-ide.org/download/current.html).

Run `sbt update-classifiers` to download dependencies including the H2 database engine used in the next section.

## Create Database

The scripts described here automate the procedure described in the [getting started guide](https://www.psma.com.au/sites/default/files/g-naf_-_getting_started_guide.pdf).
See also https://github.com/minus34/gnaf-loader as an alternative (which makes some modifications to the data).

### Download, Unpack & Generate SQL
Running:

    src/main/script/createGnafDb.sh

- downloads the G-NAF zip file to `data/` (if not found);
- unzips to `data/unzipped/` (if not found); and
- writes SQL to create the H2 database to `data/createGnafDb.sql` (`createGnafDb.sh` would require adaptation for other databases).

### Start Database Engine & SQL Client
The [H2](http://www.h2database.com/) database engine is started with:

    java -jar ~/.ivy2/cache/com.h2database/h2/jars/h2-1.4.191.jar

(the H2 jar file was put here by `sbt update-classifiers`, alternatively download the jar from the H2 web site and run it as above).
This:
- starts a web server on port 8082 serving the SQL client application (it should also open http://127.0.1.1:8082/login.jsp in a web browser);
- starts a tcp/jdbc server on port 9092; and
- starts a postgres protocol server on port 5435 (note this is different from the default port used by Postgres 5432).

The database engine is stopped with `Ctrl-C` (but not yet as it's needed for the next step).

### Run SQL
In the SQL client, enter JDBC URL: `jdbc:h2:file:~/sw/gnaf/data/gnaf`, User name: `gnaf` and Password: `gnaf`) and click `Connect` to create an empty database at this location.
This is a single file, zero-admin database. It can me moved/renamed simply by moving/renaming the `gnaf.mv.db` file.


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
        

## Generate Slick bindings
[Slick](http://slick.typesafe.com/) provides "Functional Relational Mapping for Scala".
To generate Slick mappings for the database (first disconnect any other clients):

    mkdir -p generated
    sbt
    > console
    slick.codegen.SourceCodeGenerator.main(
        Array("slick.driver.H2Driver", "org.h2.Driver", "jdbc:h2:file:~/sw/gnaf/data/gnaf", "generated", "au.com.data61.gnaf.db")
    )

This generates code in the `generated/au/com/data61/db` directory.
At this stage its not clear whether the mapping will:
1. need hand tweaks (i.e. once off generation then its part of the source code); or
2. not need hand tweaks and should be generated by the build and is not part of our source code (better if schema changes much/often).

For now we'll opt for 1 and move this into `src/main/scala`.
There has been an unsuccessful attempt at using [sbt-slick-codegen](https://github.com/tototoshi/sbt-slick-codegen)
for option 2 (see comments in `project/plugins.sbt`).

## Create Elasticsearch Index
Running:

    src/main/script/loadElasticSearch.sh

builds and runs the Scala program, transforms the output to suit Elasticsearch's 'bulk' API and loads the data to create an Elasticsearch index. Dependencies:
- Elasticsearch must be running on http://localhost:9200 (that is its default port);
- the JSON transformation tool [jq](https://stedolan.github.io/jq/).

## Example Elasticsearch Queries

Search for an exact match:

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

Get suggestions for completing the street field which match the currently entered prefix:

    $ curl -X POST 'localhost:9200/gnaf/_suggest?pretty' -d '
    { "responseName": {
      "text" : "12 TYTHER", 
      "completion" : { "field": "d61SugStreet", size: 10 }
    } }'

## Client Apps

### Elasticsearch CORS Support

Access by the client apps requires [CORS](https://en.wikipedia.org/wiki/Cross-origin_resource_sharing) to be configured in Elasticsearch.
Append to `config/elasticsearch.yml`:

    http.cors.enabled: true
    http.cors.allow-origin: "*"
        
then restart Elasticsearch.

### Web Page
The web page `src/main/webapp/index.html` provides a user interface to query the Elasticsearch index.

### Command Line
The [node.js](https://nodejs.org/) command line client require node's `request` module, which is installed with:

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

## Notes on the H2 database

If an [H2](http://www.h2database.com/) result set contains more than
[MAX_MEMORY_ROWS](http://www.h2database.com/html/grammar.html?highlight=max_memory_rows&search=MAX_MEMORY_ROWS#set_max_memory_rows),
it is spooled to disk before the first row is provided to the client.
The default is 40000 per GB of available RAM and setting a non-default value requires database admin rights (which we prefer to avoid using).
Analysis in comments in `Main.scala` shows that we need to handle result sets up to 95,004 rows, so allocating up to 3GB of heap (with `java -Xmx3G`) should avoid spooling.

## Field Statistics
- non null numberLast: 1,121,843 out of 14,126,043
- flatNumber prefix length 2, 14,099,611 nulls; suffix length 2, 4,017,287 nulls, both with lots of letter and number combinations
- level prefix length 2, 14,123,096 nulls; suffix length 2, 14,125,593 nulls
- numberFirst prefix length 3, 14,122,945 nulls; suffix length 2, 13,493,586 nulls
- numberLast prefix length 3; suffix length 2

## To Do
Add some pointers to H2 doco showing how to start a H2 with a Postgres protocol listener and connect to it with psql Postgres client. That may be a more convenient way to run `createGnafDb.sql`. Note psql cannot connect with blank username and password, so you need to create a user and grant it suitable rights.

https://www.elastic.co/guide/en/elasticsearch/guide/current/_dealing_with_null_values.html
The mapping can have a value to substitute for null values, so we can do away with that from the Scala code.

The free text query (with heuristics) takes ~0.07 sec (without heuristics it takes ~1.1 sec):

    "{"query":{"bool":{"should":[{"constant_score":{"filter":{"term":{"primarySecondary":"0"}},"boost":0.8}},{"constant_score":{"filter":{"term":{"primarySecondary":"P"}},"boost":1}},{"constant_score":{"filter":{"term":{"aliasPrincipal":"P"}},"boost":1}},{"match":{"d61NullStr":"D61_NULL"}},{"match":{"d61NullInt":"-1"}},{"term":{"stateAbbreviation":"ACT"}},{"term":{"postcode":"2601"}},{"term":{"numberFirst.number":"7"}},{"multi_match":{"query":"7","fields":["level.number^0.2","flat.number^0.4","numberFirst.number^0.5","numberLast.number^0.3","postcode^0.5"],"type":"most_fields"}},{"match":{"_all":{"query":" LONDON CCT, ","fuzziness":1,"prefix_length":2}}}],"minimum_should_match":"75%"}},"size":10}"

The equivalent fields query takes ~0.03 sec:

    "{"query":{"bool":{"should":[{"term":{"level.number":-1}},{"term":{"flat.number":-1}},{"term":{"numberFirst.prefix":"D61_NULL"}},{"term":{"numberFirst.number":"7"}},{"term":{"numberFirst.suffix":"D61_NULL"}},{"match":{"d61Street":{"query":"LONDON CCT","fuzziness":1,"prefix_length":2}}},{"term":{"postcode":"2601"}},{"term":{"stateAbbreviation":"ACT"}}],"minimum_should_match":"75%"}},"size":10}"

## Data License

Incorporates or developed using G-NAF ©PSMA Australia Limited licensed by the Commonwealth of Australia under the
[http://data.gov.au/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc/resource/09f74802-08b1-4214-a6ea-3591b2753d30/download/20160226---EULA---Open-G-NAF.pdf](Open Geo-coded National Address File (G-NAF) End User Licence Agreement).

