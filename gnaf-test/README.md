# gnaf-test

## Introduction

This sub-project provides end to end evaluation by comparing address lookups with known correct results from the database.
This approach is motivated by the fact that search tuning must be evaluated across a wide range of test data.

## Project Structure

1. The [Scala](http://scala-lang.org/) command line program `gnaf-test` produces random selections of addresses with user selected characteristics,
such as using street or locality aliases, street number prefixes, suffixes or ranges, unit or level numbers,
out of order elements (such as postcode before state) or intentional errors (to test fuzzy matching).
It outputs JSON containing the search input as an address string and the correct result as a G-NAF address without aliases or errors.
The `addressDetailPid` is not useful as the correct result because G-NAF contains addresses that are not unique (at least over the fields used here).
2. A [node.js](https://nodejs.org/en/) program [src/main/script/searchLucene.js](src/main/script/searchLucene.js) takes the above JSON, performs bulk lookups using the `gnaf-search` web service,
computes the histogram of how often the correct result is the top hit (index 0),
next hit (index 1) etc. or not in the top N hits (index -1).
Where its not the top hit the problematic input address is output for further investigation.
The histogram and problematic input addresses are output as JSON.
3. A [node.js](https://nodejs.org/en/) program [src/main/script/summary.js](src/main/script/summary.js) aggregates the above output into a single histogram. 
4. A [bash](https://www.gnu.org/software/bash/) script [src/main/script/run.sh](src/main/script/run.sh) runs all of the above.

## Dependencies

- install [node.js](https://nodejs.org/en/) and `npm` ([src/main/script/run.sh](src/main/script/run.sh) assumes that `node` executes the node.js interpreter);
- run `npm install` to install node package dependencies
