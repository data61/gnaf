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

/** 
 * return array[i] = index j where hits[i].hits[j].d61AddressNoAlias contains qBatch[i].tc.address
 * exception: if j > 0 and hits[i].hits[j].score == hits[i].hits[0].score (i.e. hit is first equal score) then return array[i] = 0
 */
var findHitIndices = (qBatch, hits) => qBatch.map((q, i) => {
  var h = hits[i];
  var j = h.hits.findIndex(h => h.d61AddressNoAlias.indexOf(q.tc.address) != -1)
  return j > 0 && aboutEqual(h.hits[j].score, h.hits[0].score) ? 0 : j; 
  // h.hits[j].score == h.hits[0].score instead of aboutEqual appears to work just as well here
});

var aboutEqual = (a, b) => Math.abs(a - b) < Math.max(a, b) * 1.e-6;


/**
 * each input test case contains 3 different queries
 * @param tc a test case
 */
var queries = tc => [ 
  {tc: tc, qstr: tc.query, desc: ''},
  {tc: tc, qstr: tc.queryPostcodeBeforeState, desc: 'PostcodeBeforeState'}, 
  {tc: tc, qstr: tc.queryTypo, desc:'Typo'}
];

var bulkQueryParam = (addresses, maxEdits) => ({addresses: addresses, numHits: numHits, fuzzy: { minLength: 5, maxEdits: maxEdits, prefixLength: 2} });

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
        var desc = (qp.fuzzy.maxEdits == 0 ? 'nofuz' : 'fuz') + q.desc;
        histMap.inc(desc, v);
        if (v != 0) errMap.get(desc).append(v, q.qstr);
      });
      if (qp.fuzzy.maxEdits == 0) {
        // on completing response for maxEdits == 0, do request with maxEdits == 2
        doRequest(bulkQueryParam(qAddr, 2));
      } else {
        // on completing response for maxEdits == 2 recurse to do next batch 
        doBatch(iter, histMap, errMap);
      }
    };
    
    function doRequest(qp) {
      request.post( { url: 'http://localhost:9040/lucene/bulkSearch', json: true, body: qp }, (error, response, hits) => {
        if (error) console.log('error', error);
        else responseHandler(qp, hits);
      });
    };
    
    // do request with qp.fuzzyMaxEdits == 0
    doRequest(bulkQueryParam(qAddr, 0));
  };
}


doBatch(batches[Symbol.iterator](), new maps.MapHist(), new maps.MapMapCont(maps.ctorMapArr));


