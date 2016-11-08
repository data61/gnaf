# gnaf-db

## Introduction

This project provides:
- scripts to load the [G-NAF data set](http://www.data.gov.au/dataset/geocoded-national-address-file-g-naf) into a relational database;
- tools to generate a [Slick](http://slick.typesafe.com/) mapping for this database; and
- a pre-generated Slick mapping.

See `gnaf/src/main/script/run.sh` for automation of the steps described below.

## Install Tools

Running sbt from the top level gnaf directory, e.g. `sbt update-classifiers` downloads dependencies including the H2 database engine used in the next section. Sbt puts the h2 jar at `~/.ivy2/cache/com.h2database/h2/jars/h2-1.4.193.jar`. Alternatively you can download it from the H2 database website.

## H2 database
H2 provides a single file, zero-admin database that can be run embedded (within the application process) or client-server.

The H2 database engine and SQL Console webapp is started with:

    java -Xmx3G -jar ~/.ivy2/cache/com.h2database/h2/jars/h2-1.4.193.jar -web

with the webapp available at `http://127.0.1.1:8082`.

### Optional Postgres Client Access
This project does not require use of a Postgres client, however this is a useful feature.
Adding `-pg` to the command above starts the Postgres protocol on port 5435 (different from the Postgres Server default of 5432 so as not to clash).

Upon the first connection using the Postgres protocol H2 runs a script to create Postgres compatible system views in order to support Postgres client commands.

First connection with admin rights to create system views:

        psql --host=localhost --port=5435 --username=gnaf --dbname=~/gnaf

Subsequent connections may use reduced access rights:

        psql --host=localhost --port=5435 --username=READONLY --dbname=~/gnaf

### Non-local Access

By default H2 only accepts connections from the localhost. Use `-webAllowOthers` and/or `-pgAllowOthers` if remote (non-localhost) access is required.


## Create Database

This section describes automation of the procedure described in the G-NAF [getting started guide](https://www.psma.com.au/sites/default/files/g-naf_-_getting_started_guide.pdf).
See also https://github.com/minus34/gnaf-loader as an alternative (which makes some modifications to the data).

### Create SQL Load Script

Running:

    src/main/script/createGnafDb.sh

- downloads the G-NAF zip file to `data/` (if not found);
- unzips to `data/unzipped/` (if not found); and
- writes SQL to create the H2 database to `data/createGnafDb.sql`.


### Run SQL Load Script
Run the SQL Load Script `data/createGnafDb.sql` using one of:

1. In the SQL Console webapp, enter JDBC URL: `jdbc:h2:file:~/gnaf`, User name: `gnaf` and Password: `gnaf`) and click `Connect`.
If a database doesn't already exist at this location an empty database is created with the given credentials as the admin user.
In the SQL box enter: `RUNSCRIPT FROM '{gnaf}/gnaf-createdb/data/createGnafDb.sql'`, substituting the full path for {gnaf}.

1. As above but paste the content of this file into the SQL box. This method displays the SQL being executed.

1. Start H2 with the `-pg` option and run the Postgres client:

	psql --host=localhost --port=5435 --username=gnaf --dbname=~/gnaf < data/createGnafDb.sql
	Password for user gnaf: gnaf

On a macbook-pro (with SSD) it takes 26 min to load the data and another 53 min to create the indexes.
The script creates a user `READONLY` with password `READONLY` that has only the `SELECT` right. This user should be used for read-only access.

## Exploring Address Data

This section is based on the first public release of the G-NAF data in February 2016. Some data anomolies may have been addressed in subsequent releases.

Find an address (fast):

    SELECT SL.*, AD.*
    FROM
        STREET_LOCALITY SL
        JOIN ADDRESS_DETAIL AD ON AD.STREET_LOCALITY_PID = SL.STREET_LOCALITY_PID  
    WHERE SL.STREET_NAME = 'BARRGANA'
        AND AD.NUMBER_FIRST = 14

The nullable type of `ADDRESS_DETAIL.STREET_LOCALITY_PID` would indicate use of a `LEFT JOIN`, however the column contains no `NULL` values so `(INNER) JOIN` can be used instead.

The same thing using the view is very slow (45892 ms):

    SELECT * FROM ADDRESS_VIEW 
    WHERE STREET_NAME = 'BARRGANA'
    AND NUMBER_FIRST = 14

but at least this is fast:

    SELECT * FROM ADDRESS_VIEW 
    WHERE ADDRESS_DETAIL_PID = 'GAWA_162637248'

Although data quality is generally very good, this shows some dodgy `STREET_LOCALITY_ALIAS` records:

    SELECT sl.STREET_NAME, sl.STREET_TYPE_CODE, sl.STREET_SUFFIX_CODE,
      sla.STREET_NAME, sla.STREET_TYPE_CODE , sla.STREET_SUFFIX_CODE 
    FROM STREET_LOCALITY_ALIAS sla, STREET_LOCALITY sl
    WHERE sla.STREET_LOCALITY_PID = sl.STREET_LOCALITY_PID
    AND sl.STREET_NAME = 'REED'

STREET_NAME | STREET_TYPE_CODE | STREET_SUFFIX_CODE | STREET_NAME | STREET_TYPE_CODE | STREET_SUFFIX_CODE
-----|--------|---|------|--------|-------
REED | STREET | S | REED STREET | SOUTH | NULL
REED | STREET | N | REED STREET | NORTH | NULL

The corresponding row in `STREET_TYPE_AUT` with `CODE` = 'SOUTH' is possibly erroneous and likewise for the other compass points.

There are inconsistencies in the usage of LOCALITY.LOCALITY_NAME and LOCALITY_ALIAS.NAME (some addresses have them reversed compared to others).

Here is an example showing a cul-de-sac with some even numbered houses:

    SELECT
      SL.STREET_NAME, SL.STREET_TYPE_CODE, SL.STREET_SUFFIX_CODE, SL.STREET_LOCALITY_PID,
      SLA.STREET_NAME, SLA.STREET_TYPE_CODE, SLA.STREET_SUFFIX_CODE, SLA.ALIAS_TYPE_CODE
    FROM
      STREET_LOCALITY as SL
      left join STREET_LOCALITY_ALIAS as SLA on SL.STREET_LOCALITY_PID = SLA.STREET_LOCALITY_PID
    WHERE SL.STREET_NAME = 'OFFICER'

STREET_NAME | STREET_TYPE_CODE | STREET_SUFFIX_CODE | STREET_LOCALITY_PID | STREET_NAME | STREET_TYPE_CODE | STREET_SUFFIX_CODE | ALIAS_TYPE_CODE
--------|----------|------|------------|------|------|------|-----
OFFICER | CRESCENT | null | ACT3379850 | null | null | null | null
OFFICER | CRESCENT | null | ACT253 | null | null | null | null
OFFICER | PLACE | null | ACT254 | OFFICER | CRESCENT | null | SYN

`STREET_LOCALITY_PID` `ACT3379850` not used in any `ADDRESS_DETAIL`. `ACT253` is used for all numbers on this street except `ACT254` used for even nos from [80 – 98].
So for most numbers only CRESCENT is acceptable, for these even nos 'PLACE' is correct but 'CRESCENT' is also acceptable.

Looking at cases where the alias has a different STREET_NAME (27119 cases):

- the vast majority appear to be spelling variants with an edit distance of 1 - exception FLAGSTONE -> WHISKEY BAY
- in some cases the number of tokens is different e.g. DE CHAIR -> DECHAIR, TWELVETREES -> TWELVE TREES
- there are aliases for SAINT X -> ST X (237), ST X -> SAINT X (26), MOUNT X -> MT X (577) and MT X -> MOUNT X (110).
Perhaps G-NAF could impose some simplifying standardisation, using SAINT/MOUNT in the main record and abreviations in the aliases? 

The following columns are always NULL:
 - ADDRESS_SITE_GEOCODE columns: BOUNDARY_EXTENT, PLANIMETRIC_ACCURACY, ELEVATION, GEOCODE_SITE_NAME and GEOCODE_SITE_DESCRIPTION;
 - LOCALITY_ALIAS.POSTCODE; and
 - DATE_RETIRED in all tables.

`ADDRESS_DETAIL.POSTCODE` is not declared `NOT NULL`, but has no `NULL` values.
`LOCALITY.PRIMARY_POSTCODE` is null for 16,029 out of 16,398 rows and '9999' for 9 rows - possibly erroneous?

There are `LOCALITY` rows:
- with duplicate `LOCALITY_NAME`; and
- without any `ADDRESS_DETAIL` rows.
  
	SELECT * FROM LOCALITY where LOCALITY_NAME = 'VERRAN'

LOCALITY_PID | DATE_CREATED | DATE_RETIRED | LOCALITY_NAME | PRIMARY_POSTCODE | LOCALITY_CLASS_CODE | STATE_PID | GNAF_LOCALITY_PID | GNAF_RELIABILITY_CODE
---|---|---|---|---|---|---|---|---
SA210005965 | 2012-05-16 | null | VERRAN | null | H | 4 | null | 6
SA1434 | 2016-01-25 | null | VERRAN | null | G | 4 | 250187718 | 5

From `LOCALITY_CLASS_AUT`, `H` & `G` mean `HUNDRED` and `GAZETTED LOCALITY`.
https://www.psma.com.au/faqs-datagovau-users explains these and suggests usage only of the `G` rows.

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

CODE | NAME | DESCRIPTION
---|---|---
ATM | AUTOMATED TELLER MACHINE | AUTOMATED TELLER MACHINE
APT | APARTMENT | APARTMENT
FLAT | FLAT | FLAT
SE | SUITE | SUITE
STU | STUDIO | STUDIO
UNIT | UNIT | UNIT
... | ... | ...
    
53 rows, many rather obscure.

#### LEVEL_TYPE_AUT
    
	SELECT * FROM LEVEL_TYPE_AUT

CODE | NAME | DESCRIPTION
---|---|---
B | BASEMENT | BASEMENT
FL | FLOOR | FLOOR
G | GROUND | GROUND
L | LEVEL | LEVEL
LB | LOBBY | LOBBY
LG | LOWER GROUND FLOOR | LOWER GROUND FLOOR
M | MEZZANINE | MEZZANINE
... | ... | ...
        
15 rows.

#### STREET_SUFFIX_AUT

	SELECT * FROM STREET_SUFFIX_AUT

CODE | NAME | DESCRIPTION | 
---|---|---
CN | CENTRAL | CENTRAL
DE | DEVIATION | DEVIATION
NE | NORTH EAST | NORTH EAST
EX | EXTENSION | EXTENSION
LR | LOWER | LOWER
... | ... | ...
        
19 rows.

#### STREET_TYPE_AUT
       
	SELECT * FROM STREET_TYPE_AUT

CODE | NAME | DESCRIPTION | 
---|---|---
HIKE | HIKE | HIKE
AWLK | AIRWALK | AIRWALK
FLATS | FLTS | FLTS
BOARDWALK | BWLK | BWLK
BOULEVARD | BVD | BVD
BOULEVARDE | BVDE | BVDE
CLOSE | CL | CL
COURT | CT | CT
CUL-DE-SAC | CSAC | CSAC
DRIVE | DR | DR
STREET | ST | ST
RIGHT OF WAY | ROFW | ROFW
... | ... | ...
        
265 rows, many rather obscure. Note reversal of column roles as discussed above and 'AWLK' exception.

### ADRESS_DETAIL Rows
- non null numberLast: 1,121,843 out of 14,126,043
- flatNumber prefix length 2, 14,099,611 nulls; suffix length 2, 4,017,287 nulls, both with lots of letter and number combinations
- level prefix length 2, 14,123,096 nulls; suffix length 2, 14,125,593 nulls
- numberFirst prefix length 3, 14,122,945 nulls; suffix length 2, 13,493,586 nulls
- numberLast prefix length 3; suffix length 2

## Exploring Geocode Data

###  ADDRESS_SITE_GEOCODE 

- RELIABILITY_CODE (descriptions quoted from p17 [GNAF Product Description](https://www.psma.com.au/sites/default/files/g-naf_product_description.pdf)):
  - 2 (13,369,902 rows), "sufficient to place geocode within address site boundary or access point
close to address site boundary"
  - 3 (186,160 rows), "sufficient to place geocode near (or possibly within) address site boundary" (generally less precise than 2)
  - no other values are used 

- BOUNDARY_EXTENT, PLANIMETRIC_ACCURACY, ELEVATION, DATE_RETIRED, GEOCODE_SITE_NAME and GEOCODE_SITE_DESCRIPTION are always null

- GEOCODE_TYPE_CODE - most of the available values in GEOCODE_TYPE_AUT are not used:

        SELECT asg.GEOCODE_TYPE_CODE , gta.NAME, count(*)
        FROM ADDRESS_SITE_GEOCODE AS asg
        JOIN GEOCODE_TYPE_AUT AS gta ON asg.GEOCODE_TYPE_CODE = gta.CODE
        GROUP BY GEOCODE_TYPE_CODE;

GEOCODE_TYPE_CODE | NAME | COUNT(*)
----|---------|-------
BC	 | BUILDING CENTROID | 201520
PCM	 | PROPERTY CENTROID MANUAL | 2307
PC	 | PROPERTY CENTROID | 9479989
PAPS | PROPERTY ACCESS POINT SETBACK | 221312
FCS	 | FRONTAGE CENTRE SETBACK | 3464774
GG	 | GAP GEOCODE | 186160

- ADDRESS_SITEs have at most 2 geocodes (58,165 have 2):

        SELECT ADDRESS_SITE_PID, count(*) AS cnt
        FROM ADDRESS_SITE_GEOCODE
        GROUP BY ADDRESS_SITE_PID
        ORDER BY cnt desc;
    
ADDRESS_SITE_PID | 	CNT 
---|---
415053318 |	2
415095264 |	2
415102559 |	2
... | ...

Looking at the first of these (ADDRESS_DETAIL_PID: GASA_414912543, 26 STRANGMAN ROAD, WAIKERIE SA 5330):

        SELECT * FROM ADDRESS_DETAIL AS ad
        JOIN ADDRESS_DEFAULT_GEOCODE AS adg ON adg.ADDRESS_DETAIL_PID = ad.ADDRESS_DETAIL_PID
        JOIN ADDRESS_SITE AS as ON as.ADDRESS_SITE_PID = ad.ADDRESS_SITE_PID
        JOIN ADDRESS_SITE_GEOCODE AS asg ON asg.ADDRESS_SITE_PID = ad.ADDRESS_SITE_PID
        WHERE ad.ADDRESS_SITE_PID = 415053318

The ADDRESS_DEFAULT_GEOCODE corresponds to one of the 2 ADDRESS_SITE_GEOCODE rows (although there is no key to link them). These have GEOCODE_TYPE_CODE: PAPS, PROPERTY ACCESS POINT SETBACK (the other row is: PC, PROPERTY CENTROID).

Note the related address with ADDRESS_DETAIL_PID = 'GASA_424344634' has an ADDRESS_DEFAULT_GEOCODE row and an ADDRESS_SITE row, but no ADDRESS_SITE_GEOCODE rows.

## Generate Slick bindings

To generate Slick mappings for the database ~/gnaf.mv.db, from the top level gnaf directory:

    sbt
    > project gnafDb
    > console
    slick.codegen.SourceCodeGenerator.main(
        Array("slick.driver.H2Driver", "org.h2.Driver", "jdbc:h2:file:~/gnaf", "generated", "au.csiro.data61.gnaf.db", "gnaf", gnaf")
    )

This generates code in: `generated/au/csiro/data61/gnaf/db/Tables.scala`.
The source file `src/main/scala/au/csiro/data61/gnaf/db/GnafTables.scala` is a very minor modification of this generated code.

