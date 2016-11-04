# gnaf-test

## Introduction

This sub-project provides end to end evaluation by comparing address lookups with known correct results from the database.
This approach is motivated by the fact that search tuning must be evaluated across a wide range of test data.

## Project Structure

1. The [Scala](http://scala-lang.org/) command line program `gnaf-test` extracts from the database random selections of addresses with user selected characteristics,
such as using street or locality aliases, street number prefixes, suffixes or ranges, unit or level numbers,
out of order elements (postcode before state) or intentional errors (to test fuzzy matching).
It outputs JSON containing the search input as an address string and the correct result as a G-NAF address without aliases or errors.
The `addressDetailPid` is not useful as the correct result because G-NAF contains addresses that are not unique (at least over the fields used here).
2. A [node.js](https://nodejs.org/en/) program [src/main/script/searchLucene.js](src/main/script/searchLucene.js) takes the above JSON, performs bulk lookups using the `gnaf-search` web service,
computes the histogram of how often the correct result is the top hit (index 0),
next hit (index 1) etc. or not in the top N hits (index -1).
Where its not the top hit the problematic input address is output for further investigation.
The histogram and problematic input addresses are output as JSON.
3. A [node.js](https://nodejs.org/en/) program [src/main/script/summary.js](src/main/script/summary.js) aggregates the above output into a single histogram. 
4. A [bash](https://www.gnu.org/software/bash/) script [src/main/script/run.sh](src/main/script/run.sh) runs all of the above.

## Configuration

Configuration is in [application.conf](src/main/resources/application.conf) and most settings can be overridden with environment variables.

## Dependencies

- install [node.js](https://nodejs.org/en/) and `npm`.
  The Ubuntu packaged versions are too old but up-to-date packages are available [here](https://github.com/nodesource/distributions).
- run `npm install` to install node package dependencies

## Results

Overall results:

	node src/main/script/summary.js stats*.json
	{"samples":4780,"histogram":[["0",4764],["1",7],["8",1],["-1",8]]}

A potential error reported for test addresses using street and locality aliases is (using [jq](https://stedolan.github.io/jq/) to filter and format):

	jq .errors stats-localityAlias-streetAlias.json
	"nofuz": {
	  "-1": [
	    "MAIDENWELL-BUNYA MOUNTAINS ROAD PIMPIMBUDGEE QLD 4615"
	  ]
	}

A non-fuzzy search gets the same score for all the top 10 hits and they are all correct matches, just not the one we were looking for.
Unfortunately G-NAF contains many duplicates with inconsistent usage of the main name and aliases.
Most reported potential errors are similarly not actual errors.
  
Baseline for following comparisons:

	node src/main/script/summary.js -nofuz stats*.json
	{"samples":956,"histogram":[["0",955],["-1",1]]}

Inputting a field out or order (postcode before state) looses bigram matches but only introduced one additional potential error:
	
	node src/main/script/summary.js -nofuzPostcodeBeforeState stats*.json
	{"samples":956,"histogram":[["0",954],["-1",2]]}

Adding a single character error and fuzzy matching also only introduced one additional potential error over the baseline:

	node src/main/script/summary.js -fuzTypo stats*.json
	{"samples":956,"histogram":[["0",954],["1",1],["-1",1]]}
	
