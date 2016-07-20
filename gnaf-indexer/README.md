# gnaf-indexer

## Introduction
This project provides a [Scala](http://scala-lang.org/) program to query the gnaf database to produce JSON and scripts to load this into [Elasticsearch](https://www.elastic.co/).

## H2 Result Set Spooling
If an [H2](http://www.h2database.com/) result set contains more than
[MAX_MEMORY_ROWS](http://www.h2database.com/html/grammar.html?highlight=max_memory_rows&search=MAX_MEMORY_ROWS#set_max_memory_rows),
it is spooled to disk before the first row is provided to the client.
The default is 40000 per GB of available RAM and setting a non-default value requires database admin rights (which we prefer to avoid using).
Analysis in comments in `Indexer.scala` shows that it needs to handle result sets up to 95,004 rows, so allocating 3GB of heap (with `java -Xmx3G`) should avoid spooling.

## Elasticsearch
Elasticsearch configuration is in `config/elasticsearch.yml`.

To prevent the node joining some other Elasticsearch cluster change the cluster name from the default 'elasticsearch':

    cluster.name: gnaf
    
Access by the client apps requires [CORS](https://en.wikipedia.org/wiki/Cross-origin_resource_sharing) to be configured (note this does not appear to work and we are currently using an nginx proxy to add a CORS header):

    http.cors.enabled: true
    http.cors.allow-origin: "*"
        
It may be necessary to increase the default heap size for Elasticsearch (at least during indexing). The [docs](https://www.elastic.co/guide/en/elasticsearch/guide/current/heap-sizing.html) suggest allocating 50% of physical memory (up to no more than a bit less than 32G). If Elasticsearch is installed from a deb/rpm package set `ES_HEAP_SIZE=2g` in the service startup script `/etc/init.d/elasticsearch`; if installed from a zip/tarball export this shell variable before running the startup script `bin/elasticsearch`.

Restart Elasticsearch for the changes to take effect.

## Configuration

Indexer configuration is in [application.conf](src/main/resources/application.conf) and most settings can be overriden with environment variables (the dburl can also be set with a command line option).

## Create Index

Running:

    src/main/script/loadElasticSearch.sh

optionally runs the Scala program (check the `if` in the source - takes about 25min with a SSD),
transforms the output to suit Elasticsearch's 'bulk' API (takes about 32min) and loads the data to create an Elasticsearch index (takes about 60min).
Dependencies:

- the JSON transformation tool [jq](https://stedolan.github.io/jq/).
  The version from the Ubuntu 15.10 or later repo is OK, the version in the Ubuntu 14.04 LTS repo is too old;
- Elasticsearch running on http://localhost:9200 (that is its default port).

## Example Queries

Get the schema/mapping (created by above script loading `src/main/script/gnafMapping.json`):

	$ curl -XGET 'localhost:9200/gnaf/?pretty'
	
Search for an exact match, retrieve at most 5 results:

    $ curl -XPOST 'localhost:9200/gnaf/_search?pretty' -d '
    {
      "query": { "match": { "street.name": "CURRONG" } },
      "size": 5
    }' 

Search for a fuzzy match against the `d61Address` field which uses a shingle filter (n-grams n = 1, 2, 3) set up in the schema/mapping.
This strongly rewards terms appearing sequentially in the correct order whilst still matching terms out-of-order.
The query string:
- should have commas and other punctuation removed (as they unnecessarily decrease the score with fuzzy matching or non-sequential term penalties);
- should have a space between the unit/flat/suite number and the street number (e.g. transform "2/7 blah street" to "2 7 blah street");
- number ranges for both unit/flat/suite numbers and street numbers should use a minus (-) separator (e.g. "2-15 9-11 blah street"). 

    curl -XPOST 'localhost:9200/gnaf/_search?pretty' -d '
    {
        "query": {
            "match": {
                "d61Address": {
                    "query": "7 LONDON CIRCUIT CITY ACT 2601",
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

## To Do

Add Code/Name from the _AUT tables as synonyms (e.g. so ST will match STREET) to the phrase suggester.
The current indexed term is the full name (which may contain spaces), so we need to add the abbreviation (which does not contain spaces).
A difference in spaces alters the number of tokens and all the following term positions resulting in problems with phrase search.
See https://www.elastic.co/guide/en/elasticsearch/guide/current/multi-word-synonyms.html, which suggest using "Simple Contraction".
However we're using shingles/ngrams rather than phrase search, so do we have the same problem? Yes I think so.
We should do contraction to the single term abreviation.
Possible negative consequences? Synonyms create the risk of spurious matches. The tables contain some unused entries (e.g. the STREET_TYPE_AUT (AWLK, AIRWALK)) and many rarely used entries; using them all as synonyms increases the risk. e.g. ATM has small edit distance from ATMA, ATKA, ATEA (street names), so contracting "AUTOMATIC TELLER MACHINE" to "ATM" could result in these street names matching AUTOMATIC TELLER MACHINEs.
Perhaps we need to be quite selective in the use of synonyms.

Other synonyms: "St" for "Saint", "Mt" for "Mount"?
The "Example Queries" section shows that this should be handled already by the inclusion of street (locality) aliases.

At some cost in terms of speed, we could prioritize primary over secondary addresses and principle over alias addresses. But maybe the default higher weight given to shorter docs is already enough?

https://www.elastic.co/guide/en/elasticsearch/guide/current/_dealing_with_null_values.html
The mapping can have a value to substitute for null values, so we can do away with that from the Scala code.
