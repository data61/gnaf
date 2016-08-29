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
 * Usage: node src/main/node/searchLucene.js input.json
 * Input: one address per line. Performs bulk lookup using gnaf-lucene-service.
 * TODO: add proper command line option handling, add options to set numHits and bulk
 */
var path = process.argv[2]; // 0 -> node; 1 -> src/main/script/search.js; 2 -> input.json
var numHits = 10;

var addr = JSON.parse(fs.readFileSync(path, "utf8"));
// console.log('addr', addr);

var bulk = Math.floor(50/3);
var batches = [];
for (i = 0; i < addr.length; i += bulk) batches.push(addr.slice(i, Math.min(i + bulk, addr.length)));
// console.log('bulk', bulk, 'batch sizes', batches.map(b => b.length));

/** return array[i] = index j where hits[i].hits[j].d61AddressNoAlias contains qBatch[i].tc.address */
var findHitIndices = (qBatch, hits) => qBatch.map( (q, i) =>
  hits[i].hits.findIndex(h => h.d61AddressNoAlias.indexOf(q.tc.address) != -1)
);

/**
 * each input test case contains 3 different queries
 * @param tc a test case
 */
var queries = tc => [ 
  {tc: tc, qstr: tc.query, desc: ''},
  {tc: tc, qstr: tc.queryPostcodeBeforeState, desc: 'PostcodeBeforeState'}, 
  {tc: tc, qstr: tc.queryTypo, desc:'Typo'}
];

var bulkQueryParam = (addresses, fuzzyMaxEdits) => ({addresses: addresses, numHits: numHits, minFuzzyLength: 5, fuzzyMaxEdits: fuzzyMaxEdits, fuzzyPrefixLength: 2});

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
    
    // array of batch.length * 3: { tc: tc, qstr: query address string, desc: description }
    var qBatch = batch.flatMap(queries);
    // console.log('batch.length', batch.length, 'qBatch.length', qBatch.length); // , 'qBatch', qBatch);
    var qAddr = qBatch.map(x => x.qstr); // array of query addresses from qBatch
    
    function responseHandler(qp, hits) {
      var idxs = findHitIndices(qBatch, hits);
      // console.log('idxs', idxs);
      // histogram(histMap, idxs);
      // console.log('histMap', histMap);
      idxs.forEach((v, i) => {
        var q = qBatch[i];
        var desc = (qp.fuzzyMaxEdits == 0 ? 'nofuz' : 'fuz') + q.desc;
        histMap.inc(desc, v);
        if (v != 0) errMap.get(desc).append(v, q.qstr);
      });
      if (qp.fuzzyMaxEdits == 0) {
        // on completing response for qp.fuzzyMaxEdits == 0, do request with qp.fuzzyMaxEdits == 2
        doRequest(bulkQueryParam(qAddr, 2));
      } else {
        // on completing response for qp.fuzzyMaxEdits == 2 recurse to do next batch 
        doBatch(iter, histMap, errMap);
      }
    };
    
    function doRequest(qp) {
//      console.log('doRequest: qp', qp);
      request.post( { url: 'http://localhost:9040/lucene/bulkSearch', json: true, body: qp }, (error, response, hits) => {
        if (error) console.log('error', error);
        else {
//          console.log('statusCode', response.statusCode);
//          for (i = 0; i < hits.length; ++i) {
//            console.log('addr', qp.addresses[i]);
//            var h = hits[i];
//            for (j = 0; j < h.hits.length; ++j) {
//              console.log('hits[' + i + '][' + j + ']', h.hits[j]);
//            }
//          }
          responseHandler(qp, hits);
        }
      });
    };
    
    // do request with qp.fuzzyMaxEdits == 0
    doRequest(bulkQueryParam(qAddr, 0));
  };
}


doBatch(batches[Symbol.iterator](), new maps.MapHist(), new maps.MapMapCont(maps.ctorMapArr));


