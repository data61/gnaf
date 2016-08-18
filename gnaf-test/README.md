# gnaf-test

## Introduction

Most of the code of the gnaf project is involved with loading G-NAF data into a database and from there into Elasticsearch.
This sub-project provides end to end evaluation by comparing Elasticsearch address lookups with known correct results from the database.
This approach is motivated by:
- the fact that the quality (precision & recall) of searches cannot be tested on small test data sets,
because results of a search for a single address depend on relative term frequencies across the entire data set; and
- search tuning must be evaluated across a wide range of test data.

## Outline

1. The [Scala](http://scala-lang.org/) command line program `gnaf-test` produces random selections of addresses with user selected characteristics,
such as using street or locality aliases, street number prefixes, suffixes or ranges, unit or level numbers,
out of order elements (such as postcode before state) or intentional errors (to test fuzzy matching).
It outputs JSON containing the search input as an address string and the correct result as a G-NAF address without aliases or errors.
The `addressDetailPid` is not useful as the correct result because G-NAF contains addresses that are not unique (at least over the fields used here).
2. A [node.js](https://nodejs.org/en/) program `src/main/script/search.js` takes the above JSON, performs bulk Elasticsearch queries,
computes the histogram of how often the correct result is the top hit (index 0),
next hit (index 1) etc. or not in the top N hits (index -1).
Where its not the top hit the problematic input address is output for further investigation.
The histogram and problematic input addresses are output as JSON.
3. A [node.js](https://nodejs.org/en/) program `src/main/script/summary.js` to aggregate the above output into a single histogram. 
4. A [bash](https://www.gnu.org/software/bash/) script `src/main/script/run.sh` to run all of the above.

