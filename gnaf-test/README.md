# gnaf-test

## Introduction

Most of the code of this project is involved with loading G-NAF data into a database and from there into Elasticsearch.
Rather than providing unit tests of low level functions in this code, this sub-project provides end to end testing.
This approach is motivated by the fact that the quality (precision & recall) of searches cannot be tested on small test data sets,
because results of a search for a single address depend on relative term frequencies across the entire data set.

1. The [Scala](http://scala-lang.org/) command line program `gnaf-test` produces random selections of addresses with user selected characteristics,
such as using street or locality aliases, street number prefixes, suffixes or ranges, unit or level numbers,
out of order elements (such as postcode before state) or intentional errors (to test fuzzy matching).
It outputs JSON containing the search input as an address string and the correct result as a G-NAF address without aliases or errors (the addressDetailPid is not useful as the correct result because G-NAF contains duplicate addresses).
2. A [node.js](https://nodejs.org/en/) program `src/main/script/search.js` takes the above JSON, performs bulk Elasticsearch queries,
computes the histogram of how often the correct result is the top hit (index 0),
next hit (index 1) etc. or not in the top N hits (index -1).
Where its not the top hit the problematic input address is output for further investigation.
The histogram and problematic input addresses are output as JSON.
3. A [node.js](https://nodejs.org/en/) program `src/main/script/summary.js` to aggregate the above output into a single histogram. 
4. A [bash](https://www.gnu.org/software/bash/) script `src/main/script/run.sh`:
   - runs `gnaf-test` multiple times with different options to produce sets of test addresses;
   - runs `search.js` on each set of addresses;
   - runs `summary.js` to aggregate the results.

## Results
Batches of tests are run without rerunning `gnaf-test` each time to reuse the same random selection of addresses.
Tests before `testResults7` accepted only the `addressDetailPid` matching the record used to generate the query,
but from then on `d61AddressNoAlias` text matching is accepted as a correct hit (as any duplicate record is equally acceptable).

### results in folder testResults1

Testing without errors in the input and without fuzzy matching has shown that the main source of errors is
where the input address contains no street number but the top hit does contain a spurious street number.
Aggregate results [index where correct result is found or -1 if not found , occurrence count]:

	{"samples":900,"histogram":[[0,833],[3,11],[1,12],[4,5],[-1,20],[6,3],[8,2],[5,4],[7,1],[2,9]]}
	
So we're only getting the correct top hit 833 times out of 900 samples or 93% of the time.
Backup this index: `tar cvfz gnaf4shardsv1.tar.gz -C /var/lib/elasticsearch/neilsElasSrch/ nodes`

### results in folder testResults2

Same index as above, but add a bool query term `should: numberFirst.number: -1, minimum_should_match: 0` to prefer missing numbers.
This got a worse result with 628/900 correct top hits.

	{"samples":900,"histogram":[[1,70],[0,628],[3,29],[4,16],[6,10],[2,34],[5,16],[-1,74],[8,8],[9,5],[7,10]]}

We could try lowering the weight on this term, but this is getting a bit arbitrary.

### results in folder testResults3

Add a `D61_NO_NUM` token to represent the absence of a street number in the index and add it to all queries.

	{"samples":900,"histogram":[[0,832],[3,8],[1,21],[4,8],[2,10],[5,7],[7,2],[-1,10],[9,1],[6,1]]}
	
Score for top hits is 1 less than testResults1, other results are slightly better, certainly not a significant improvement.
Backup this index: `tar cvfz gnaf4shardsv2.tar.gz -C /var/lib/elasticsearch/neilsElasSrch/ nodes`

### results in folder testResults4

Undo above changes. The index is slightly different to testResult1 with the change from loadElasticsearch.{jq,js}, but it shouldn't be significant.
Appears to be slightly worse. Backup loadElasticsearch.js to testResults4. Backup index to `gnaf4shardsv3.tar.gz`.

	{"samples":900,"histogram":[[0,831],[3,10],[1,14],[4,7],[-1,18],[8,2],[5,5],[7,4],[2,7],[6,2]]}

### results in folder testResults5

This test puts all the address fields for the main (non-alias) address in a single string rather than separate strings for unit+level, street, locality.
This allows {2,3}gram matches across these boundaries, whereas that was previously prevented.
Backup loadElasticsearch.js to testResults5.
Backup index: `tar cvfz '/media/bac003/Seagate Backup Plus Drive/gnaf/indexBackups/gnaf4shardsv4.tar.gz' -C /var/lib/elasticsearch/neilsElasSrch/ nodes`

	{"samples":900,"histogram":[[0,827],[1,14],[4,6],[-1,23],[6,2],[8,4],[5,4],[7,4],[2,7],[3,8],[9,1]]}
	
Doesn't help and will result in more ngram terms in the query, so best avoided.


### results in folder testResults6

Change from 3-grams to 2-grams. Appears no worse, so to be preferred as simpler. We'll revisit this after we've got the best results we can with 2-grams.

	{"samples":900,"histogram":[[0,831],[3,11],[1,13],[4,7],[-1,18],[8,2],[5,6],[7,3],[6,2],[2,7]]}
	
Backup index: `tar cvfz '/media/bac003/Seagate Backup Plus Drive/gnaf/indexBackups/gnaf4shardsv5.tar.gz' -C /var/lib/elasticsearch/neilsElasSrch/ nodes`

### results in folder testResults7

We've noted that many of the error cases have no street number.
Some of these are non-unique so there are multiple correct top hits and we're just not getting the one with the addressDetailPid we hoped for.
Here we add a `d61AddressNoAlias` field with the content of `d61Address` excluding street and locality aliases.
The test for the correct hit accepts a result exactly matching this text (rather than the addressDetailPid).

	{"samples":900,"histogram":[[0,842],[1,12],[3,10],[4,5],[-1,15],[6,2],[8,1],[2,7],[5,4],[7,2]]}


gnaf-test needs to be jacked up to output 2 strings, the query and the correct address text without aliases to match d61AddressNoAlias.
However, re-running gnaf-test will give us different addresses and so give results not directly comparable to the tests run so far.
Lets delay this change until we've investigated the next issue.

### results in folder testResults8

In some cases we're getting a street number in the top hit that we do not want so it really is incorrect.
In cases I've looked at, the reported score is identical for the hits with and without numbers.
Post process search result sorting hits on score first (as returned by elasticsearch) but breaking ties sorting on shortest d61AddressNoAlias first.
This is the best result yet.
This is a bit dicey because idf scores can be different across different shards so scoring is not exact
(there is an option for global idf but this comes with a performance penalty).

	{"samples":900,"histogram":[[0,846],[2,8],[1,11],[3,9],[4,5],[-1,15],[6,2],[7,1],[5,3]]}


### results in folder testResults9
Re-indexed with:
- "d61Address" with "norms": { "enabled": true } (this is documented as the default, but we're not seeing the expected field length normalisation)
- "d61Address" with a "raw" field without the shingle filter (to compare search strategies other than n-gram)
- "d61AddressNoAlias" as a single string rather than an array like "d61Address"


	{"samples":900,"histogram":[[0,846],[2,8],[1,11],[3,9],[4,4],[-1,15],[6,3],[7,1],[5,3]]}
	
Still getting identical scores for different lengths, so explicitly adding "norms" has had no effect.
We'll remove it when we next re-index. 
Sorting on score then shortest length is helping, but for "FITZROY DEVELOPMENTAL ROAD RHYDDING QLD 4718"
the top 2 hits have  "_score" : 11.574617 and have street numbers,
the next 2 hits have "_score" : 11.528666 one with a street number and one without.

We could:
1. discount scores for longer matches (bit of an arbitrary hack risking making some cases worse); or
2. keep the above tie breaking and accept longer matches as satisfactory for cases where the score differs
(elasticsearch by default does not guarantee the same idf scores across shards, but we could change that); or
3. remove the tie break and accept more cases of longer matches.

I'm favouring 2 as helpful and safe.
Lets adjust the test to accept a hit with the desired text as a substring.
As mentioned above, this is using a new test data set.
 
	{"samples":899,"histogram":[[0,891],[3,1],[1,3],[-1,2],[7,1],[2,1]]}

Best results so far, but note that we're improving the test not the search!

Results in detail:

	for i in stats*.json; do echo $i; cat $i; echo ""; done
	
	stats.json
	{"histogram":[[0,153]],"errors":[]}

	stats-localityAlias.json
	{"histogram":[[0,198],[3,1]],"errors":[[3,["BRUCE HIGHWAY COLOSSEUM QLD 4676"]]]}

This search is for "addressDetailPid" : "GAQLD162951586" with "d61AddressNoAlias" : "BRUCE HIGHWAY GINDORAN QLD 4676".
Hits 0 - 2 also have locality alias COLOSSEUM so they are just as good matches, but their main locality is LOWMEAD QLD 4676
so they are not accepted by the test.

	stats-localityAlias-numberAdornments.json
	{"histogram":[[0,125],[1,1]],"errors":[[1,["1286-1294 COTHERSTONE ROAD CAPELLA QLD 4723"]]]}

The item generated by `gnaf-test` is:

	  {
	    "query": "1286-1294 COTHERSTONE ROAD CAPELLA QLD 4723",
	    "addressDetailPid": "GAQLD425202140",
	    "address": "1286-1294 COTHERSTONE ROAD KHOSH BULDUK QLD 4723"
	  }

The top search hit has main locality "CAPELLA QLD 4723" so is at least equally valid as the expected address above.
This and the previous test are picking up issues with duplicates in G-NAF with inconsistent use LOCALITY and LOCALITY_ALIAS;
rather than issues with the search.

	stats-localityAlias-streetAlias.json
	{"histogram":[[0,122],[-1,2],[1,2],[7,1],[2,1]],"errors":[[-1,["555 GRINDSTONE ROAD GLAN DEVON QLD 4615","BEAUDESERT-NERANG ROAD TABRAGALBA QLD 4275"]],[1,["772 AXEDALE ROAD AXEDALE VIC 3557","1 RANNOCK AVENUE TYNDALE NSW 2463"]],[7,["MT OGG ROAD ROLLESTON QLD 4702"]],[2,["STANTHORPE-INGLEWOOD ROAD CEMENT MILLS QLD 4387"]]]}

The item generated by `gnaf-test` is:
	
	  {
	    "query": "555 GRINDSTONE ROAD GLAN DEVON QLD 4615",
	    "addressDetailPid": "GAQLD157635231",
	    "address": "555 BULLCAMP ROAD BULLCAMP QLD 4615"
	  }
  
We're not getting some street variant hits, so re-index with street numbers added to street aliases.

	stats-localityAlias-unitLevel.json
	{"histogram":[[0,107]],"errors":[]}
	
	stats-numberAdornments.json
	{"histogram":[[0,65]],"errors":[]}
	
	stats-streetAlias.json
	{"histogram":[[0,73]],"errors":[]}
	
	stats-unitLevel.json
	{"histogram":[[0,48]],"errors":[]}

### results in folder testResults10

Re-indexed with:
- removed "norms": { "enabled": true } (ws default, had no effect)
- added street number to street aliases

This corrected 3 errors:

	{"samples":899,"histogram":[[0,894],[3,1],[1,1],[7,1],[-1,1],[2,1]]}
	
Results in detail:

	for i in stats*.json; do echo $i; cat $i; echo ""; done
	
	stats.json
	{"histogram":[[0,153]],"errors":[]}
	
	stats-localityAlias.json
	{"histogram":[[0,198],[3,1]],"errors":[[3,["BRUCE HIGHWAY COLOSSEUM QLD 4676"]]]}
	
	stats-localityAlias-numberAdornments.json
	{"histogram":[[0,125],[1,1]],"errors":[[1,["1286-1294 COTHERSTONE ROAD CAPELLA QLD 4723"]]]}

As seen in the previous section, the above two apparent errors are actually OK.
	
	stats-localityAlias-streetAlias.json
	{"histogram":[[0,125],[7,1],[-1,1],[2,1]],"errors":[[7,["MT OGG ROAD ROLLESTON QLD 4702"]],[-1,["BEAUDESERT-NERANG ROAD TABRAGALBA QLD 4275"]],[2,["STANTHORPE-INGLEWOOD ROAD CEMENT MILLS QLD 4387"]]]}

The item generated by `gnaf-test` is:

	  {
	    "query": "MT OGG ROAD ROLLESTON QLD 4702",
	    "addressDetailPid": "GAQLD161246086",
	    "address": "MOUNT OGG ROAD CONSUELO QLD 4702"
	  }	

The top hit "MT OGG ROAD CONSUELO QLD 4702" is correct.
All the top hits have the street as "MT OGG ROAD" with no street alias,
but the hit accepted by the test has the street as "MOUNT OGG ROAD" with an alias of "MT OGG ROAD".
Another issue caused by inconsistent usage of aliases.

	  {
	    "query": "BEAUDESERT-NERANG ROAD TABRAGALBA QLD 4275",
	    "addressDetailPid": "GAQLD161062438",
	    "address": "BEAUDESERT NERANG ROAD BOYLAND QLD 4275"
	  }

This addressDetailPid has:

	"d61Address" : [ "BEAUDESERT NERANG ROAD", "BOYLAND QLD 4275", "BEAUDESERT-NERANG ROAD", "TABRAGALBA QLD 4275" ]

The top hit is correct but has locality "BIDDADDABA" - another alias issue.

	{
	    "query": "STANTHORPE-INGLEWOOD ROAD CEMENT MILLS QLD 4387",
	    "addressDetailPid": "GAQLD163299506",
	    "address": "STANTHORPE INGLEWOOD ROAD TERRICA QLD 4387"
	}
	
The top 3 hits are correct (with no street number) and have the same score, first has locality TERRICA, next 2 have locality WARROO.
Here tie breaking with shortest first has picked the wrong locality, but its still a correct result.

	stats-localityAlias-unitLevel.json
	{"histogram":[[0,107]],"errors":[]}
	
	stats-numberAdornments.json
	{"histogram":[[0,65]],"errors":[]}
	
	stats-streetAlias.json
	{"histogram":[[0,73]],"errors":[]}
	
	stats-unitLevel.json
	{"histogram":[[0,48]],"errors":[]}
	
Its looking pretty good, but we don't have a solid way of ensuring that a query without a street number will prefer results without street numbers.

Now lets try postcode out of order and spelling mistakes with fuzzy searches.
