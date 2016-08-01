# gnaf-indexer

## Introduction
This project provides a [Scala](http://scala-lang.org/) program to query the gnaf database to produce JSON and scripts to load this into [Elasticsearch](https://www.elastic.co/).

## H2 Result Set Spooling
If an [H2](http://www.h2database.com/) result set contains more than
[MAX_MEMORY_ROWS](http://www.h2database.com/html/grammar.html?highlight=max_memory_rows&search=MAX_MEMORY_ROWS#set_max_memory_rows),
it is spooled to disk before the first row is provided to the client.
The default is 40000 per GB of available RAM and setting a non-default value requires database admin rights (which we prefer to avoid using).
Analysis in comments in `Indexer.scala` shows that it needs to handle result sets up to 95,004 rows, so allocating 3GB of heap (with `java -Xmx3G`) should avoid spooling.

## Elasticsearch Configuration
Elasticsearch configuration is in `config/elasticsearch.yml`.

To prevent the node joining some other Elasticsearch cluster change the cluster name from the default 'elasticsearch':

    cluster.name: gnaf
    
The default search queue size of 1000 is sufficient to handle a bulk lookup of 100 addresses, but is blown with 2 requests each for 100 addresses. Increasing the queue size to 5000 is more than sufficient to handle 3 requests each for 100 addresses. Runtime is proportional to the number of addresses, so there is no point in going beyond batches of 50 or so, just one at a time.

    threadpool: search: queue_size: 5000

Access by the client apps requires [CORS](https://en.wikipedia.org/wiki/Cross-origin_resource_sharing) to be configured (note this does not appear to work and we are currently using an nginx proxy to add a CORS header - see below):

    http.cors.enabled: true
    http.cors.allow-origin: "*"
        
It may be necessary to increase the default heap size for Elasticsearch. The [docs](https://www.elastic.co/guide/en/elasticsearch/guide/current/heap-sizing.html) suggest allocating 50% of physical memory (up to no more than a bit less than 32G). Observations of heap usage (RSS in top, with max heap set to considerably larger than the observed usage):
- indexing on a MacBook Pro (with 8G memory, creating an index with 8 shards): 2G
- bulk searches on a c3.2xlarge (8 cores, 15G memory): 4G
- bulk searches on a c3.xlarge or c4.xlarge (4 cores, 7G memory): 1.4G (may not have reached its max, so allow 2G)
 
The max heap size should be set to something between 50% larger than these values and up to 50% of physical memory, e.g.:
- `ES_HEAP_SIZE=6g` on a c3.2xlarge (15G memory)
- `ES_HEAP_SIZE=3G` on a c3.xlarge, c4.xlarge or MacBook Pro (7 - 8G memory)
 
If Elasticsearch is installed from a deb/rpm package set `ES_HEAP_SIZE` in the local config script `/etc/default/elasticsearch`; if installed from a zip/tarball export this shell variable before running the startup script `bin/elasticsearch`.

Elasticsearch [configuration](https://www.elastic.co/guide/en/elasticsearch/reference/current/setup-configuration.html) documentation suggests tuning various OS parameters to avoid swapping, lock pages in memory, increase the number of file descriptors etc.; however this application appears to be CPU constrained rather than being limited by memory, files, i/o etc. and this is probably not beneficial.

Restart Elasticsearch for the changes to take effect.

## Indexer
### Configuration

Indexer configuration is in [application.conf](src/main/resources/application.conf) and most settings can be overriden with environment variables (the dburl can also be set with a command line option).

Elasticsearch configuration specific to the `gnaf` index is in [gnafMapping.json](src/main/script/gnafMapping.json). This includes field mapping, tokenizers, analyzers and the number of shards.

### Running

    src/main/script/loadElasticsearch.sh

optionally runs the Scala program (check the `if` in the source - takes about 25min with a SSD),
transforms the output to suit Elasticsearch's 'bulk' API (takes about 32min) and loads the data to create an Elasticsearch index (takes about 60min). Dependencies:

- the JSON transformation tool [jq](https://stedolan.github.io/jq/).
  The version from the Ubuntu 15.10 or later repo is OK, the version in the Ubuntu 14.04 LTS repo is too old;
- Elasticsearch running on http://localhost:9200 (that is its default port).

## Usage of the Index
This section demonstrates usage the Elasticsearch index created above.

### Example Queries
Notes: `curl` defaults to `-XGET` if no data is sent or `-XPOST` otherwise, so these are omitted from the commands below.
When JSON data is sent it may be more correct to add `-H 'Content-Type: application/json'` and to use `--data-binary` rather than `-d` or `--data`, however the shorter commands shown here work.

Get the schema/mapping (which was set from `gnafMapping.json`):

    $ curl 'localhost:9200/gnaf/?pretty'
	
Search for an exact match, retrieve at most 5 results:

    $ curl 'localhost:9200/gnaf/_search?pretty' -d '
    {
      "query": { "match": { "street.name": "CURRONG" } },
      "size": 5
    }' 

Search for a fuzzy match against the `d61Address` field which uses a shingle filter (n-grams n = 1, 2, 3) set up in the schema/mapping.
This strongly rewards terms appearing sequentially in the correct order whilst still matching terms out-of-order.

    curl 'localhost:9200/gnaf/_search?pretty' -d '
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

The query string:
- should have commas and other punctuation removed (as they unnecessarily decrease the score with fuzzy matching or non-sequential term penalties);
- should have a space between the unit/flat/suite number and the street number (e.g. transform "2/7 blah street" to "2 7 blah street");
- number ranges for both unit/flat/suite numbers and street numbers should use a minus (-) separator (e.g. "2-15 9-11 blah street"). 

Search for addresses within 200m of a location:

    curl 'localhost:9200/gnaf/_search?pretty' -d '
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
    
Search for multiple addresses (bulk lookup) retrieving only the top ranked match for each:

    curl 'localhost:9200/gnaf/_msearch' -d '
    {}
    {"query":{"match":{"d61Address":{"query":"405 Camberwell Road Camberwell VIC 3124","fuzziness":2,"prefix_length":2}}},"rescore":{"query":{"rescore_query":{"match":{"d61Address":{"query":"405 Camberwell Road Camberwell VIC 3124"}}},"query_weight":0}},"size":1}
    {}
    {"query":{"match":{"d61Address":{"query":"1 8 Blackburn Scout Hall Blackburn VIC 3130","fuzziness":2,"prefix_length":2}}},"rescore":{"query":{"rescore_query":{"match":{"d61Address":{"query":"1 8 Blackburn Scout Hall Blackburn VIC 3130"}}},"query_weight":0}},"size":1}
    {}
    {"query":{"match":{"d61Address":{"query":"St Thomas Church Hall Werribee VIC 3030","fuzziness":2,"prefix_length":2}}},"rescore":{"query":{"rescore_query":{"match":{"d61Address":{"query":"St Thomas Church Hall Werribee VIC 3030"}}},"query_weight":0}},"size":1}
    {}
    {"query":{"match":{"d61Address":{"query":"2 96 Voltri Street Mentone VIC 3194","fuzziness":2,"prefix_length":2}}},"rescore":{"query":{"rescore_query":{"match":{"d61Address":{"query":"2 96 Voltri Street Mentone VIC 3194"}}},"query_weight":0}},"size":1}
    '

### Performance

The bulk lookup query shown above was used to observe the performance of the index on various hardware.
A mixture of 307 valid and underspecified (e.g. organisation name instead of street address, missing street number) address queries was formatted as shown above into 3 files of about 100 addresses each.
These were executed with `curl 'localhost:9200/gnaf/_msearch' --data-binary @filename` either 1, 2 or all 3 at a time to lookup about 100, 200 or 300 addresses concurrently (`-d` does not work in this case).
- runtime was proportional to the number of addresses being looked up concurrently
  - c3.2xlarge: 0.24 sec/address
  - c4.xlarge: 0.40 sec/address
  - macbook pro: 0.41 sec/address
  - c3.xlarge: 0.51 sec/address
- the 8 core machine had twice the throughput of the 4 core machines (all cores were fully utilised)
- heap usage was < 2G for 4 cores and about 4G for 8 cores
- buff/cache (as reported by `top` and used by the memory mapped Lucene index for each shard) was around 2G
- the c4 machine (without local SSDs) was faster than the c3 (using local SSDs), so it appears that performance is not limited by i/o speed (or at least by latency). The bottleneck appears to be CPU.


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
