var request = require('request');
var fs = require('fs');
var maps = require('./Maps.js');

Array.prototype.flatMap = function(f) {
  return this.map(f).flatten();
}
Array.prototype.flatten = function() {
  return Array.prototype.concat.apply([], this);
}

/**
 * Usage: node src/main/node/searchEs.js input.json
 * Input: one address per line. Performs bulk lookup using Elasticsearch index created by gnaf-indexer.
 * TODO: add proper command line option handling, add options to set numHits and bulk
 */
var path = process.argv[2]; // 0 -> node; 1 -> src/main/script/search.js; 2 -> input.json
var numHits = 10;

var addr = JSON.parse(fs.readFileSync(path, "utf8"));
// console.log('addr', addr);

var bulk = 10;
var batches = [];
for (i = 0; i < addr.length; i += bulk) batches.push(addr.slice(i, Math.min(i + bulk, addr.length)));
// console.log('batches', batches);

/** return array[i] = index j where esHits.responses[i].hits.hits[j].fields.d61AddressNoAlias[0] contains qBatch[i].tc.address */
var findHitIndices = (qBatch, esHits) => qBatch.map( (q, i) =>
  esHits.responses[i].hits.hits.findIndex(h => h.fields.d61AddressNoAlias[0].indexOf(q.tc.address) != -1)
);

/**
 * @return non-fuzzy elasticsearch query
 * 
 * @param qstr a query address string
 * 
 * If we don't specify "fields" we get _source.d61AddressNoAlias as a String
 * however if we do specify "fields" _source is omitted and we get fields.d61AddressNoAlias as an array of Strings (with just 1 element).
 */
var esNoFuz = qstr =>
({
  query:{ match:{ d61Address: qstr }},
  fields:[ "d61AddressNoAlias" ],
  size:numHits
});

/**
 * @return fuzzy elasticsearch query
 * 
 * @param qstr a query address string
 */
var esFuz = qstr =>
({
  query:{ match:{ d61Address:{ query: qstr, fuzziness: 2, prefix_length: 2 }}},
  // rescore:{ query:{ rescore_query:{ match:{ d61Address:{ query: qstr }}}, query_weight: 0 }}, why did I think this was a good idea???
  fields: [ "d61AddressNoAlias" ],
  size: numHits
});

/** @return array elements for a non-fuzzy and a fuzzy search */
var mkEs = (tc, qstr, desc) => 
[
  { tc: tc, qstr: qstr, qes: esNoFuz(qstr), desc: 'nofuz' + desc },
  { tc: tc, qstr: qstr, qes: esFuz(qstr),   desc: 'fuz' + desc }
];

/**
 * 6 combinations of queries: 3 different queries with and without fuzzy search
 * @return array of { tc: tc, qstr: query address string, qes: elasticsearch query, desc: description }
 * @param tc a test case
 */
var queries = tc => [
  mkEs(tc, tc.query, ''), 
  mkEs(tc, tc.queryPostcodeBeforeState, 'PostcodeBeforeState'), 
  mkEs(tc, tc.queryTypo, 'Typo')
].flatten();


// comparitor to sort by score then shortest d61AddressNoAlias first
var scoreThenLength = (a, b) =>
  b._score != a._score ? b._score - a._score
                       : a.fields.d61AddressNoAlias[0].length - b.fields.d61AddressNoAlias[0].length;

// sort each esHits.responses[i].hits.hits according to comparitor cmp
var sortHits = (esHits, cmp) => {
  esHits.responses.forEach(r => r.hits.hits.sort(cmp));
  return esHits;
};

var done = (histMap, errMap) => console.log(JSON.stringify({ histogram: histMap.object(), errors: errMap.object() }));

/**
 * Process a batch and on completion recursively do the next.
 * @param iter provides next batch
 * @param histMap test description -> histogram
 *       where histogram is (index of correct hit (0 in best case) -> occurrence count for this index)
 * @param errMap test description -> index of correct hit -> array of addresses with this index
 */
function doBatch(iter, histMap, errMap) {
  var x = iter.next();
  if (x.done) done(histMap, errMap);
  else {    
    var batch = x.value;
    
    // array of batch.length * 6:
    //   { tc: tc, qstr: query address string, qes: elasticsearch query, desc: description }
    var qBatch = batch.flatMap(queries);
    // console.log('qBatch', qBatch);
    
    var esBulk = qBatch.flatMap(q => [ '{}', JSON.stringify(q.qes) ]).join('\n') + '\n';
    // console.log('esBulk', esBulk);
  
    request.post( { url: 'http://localhost:9200/gnaf/_msearch', body: esBulk }, (error, response, body) => {
      if (error) console.log('error', error)
      else {
        // console.log('statusCode', response.statusCode, 'body', body);
        var esHits = sortHits(JSON.parse(body), scoreThenLength);
        // console.log('esHits', JSON.stringify(esHits));
        var idxs = findHitIndices(qBatch, esHits);
        // console.log('idxs', idxs);
        // histogram(histMap, idxs);
        // console.log('histMap', histMap);
        idxs.forEach((v, i) => {
          var q = qBatch[i];
          histMap.inc(q.desc, v);
          if (v != 0) errMap.get(q.desc).append(v, q.qstr);
        });
        doBatch(iter, histMap, errMap);
      }
    });
  };
}


doBatch(batches[Symbol.iterator](), new maps.MapHist(), new maps.MapMapCont(maps.ctorMapArr));


