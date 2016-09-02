# gnaf-extractor

## Introduction
This project queries the gnaf database to produce JSON address data (for consumption by gnaf-indexer).
`src/main/script` contains obsolete scripts to load the output into Elasticsearch.


## H2 Result Set Spooling
If an [H2](http://www.h2database.com/) result set contains more than
[MAX_MEMORY_ROWS](http://www.h2database.com/html/grammar.html?highlight=max_memory_rows&search=MAX_MEMORY_ROWS#set_max_memory_rows),
it is spooled to disk before the first row is provided to the client.
The default is 40000 per GB of available RAM and setting a non-default value requires database admin rights (which we prefer to avoid using).
Analysis in comments in `Extractor.scala` shows that it needs to handle result sets up to 95,004 rows, so allocating 3GB of heap (with `java -Xmx3G`) should avoid spooling.

## Configuration

Configuration is in [application.conf](src/main/resources/application.conf) and most settings can be overridden with environment variables.
The database URL can also be set with a command line option (overriding the above, use `--help` for details).

## Running and Usage

See `gnaf/src/main/script/run.sh`.

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
