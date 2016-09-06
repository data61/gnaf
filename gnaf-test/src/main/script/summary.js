var fs = require('fs');

/**
 * Usage: node src/main/node/summary.js files ...
 * 
 * The test results (our input) are keyed by a description of the test data.
 * By default we sum the data for all descriptions except 'nofuzTypo', which is excluded because data
 * potentially containing typos should be searched with 'fuz'.
 * If a "-{desc}" precedes files then we sum only the descriptions matching this {desc}.  
 */
var argIdx = 2; // 0 -> node; 1 -> src/main/script/summary.js; 2 -> [-desc] files ...
var descMatch = process.argv[argIdx].startsWith('-') ? process.argv[argIdx++].substring(1) : null;
var descPred = descMatch ? desc => desc == descMatch : desc => desc != 'nofuzTypo';

var m = new Map();
for (; argIdx < process.argv.length; ++argIdx) {
  var stats = JSON.parse(fs.readFileSync(process.argv[argIdx], "utf8"));
  for (desc in stats.histogram) {
    if (descPred(desc)) {
      var o = stats.histogram[desc];
      for (p in o) histAdd(m, p, o[p]);
    }
  }
}

var sum = 0;
for (i of m.values()) sum += i;
console.log(JSON.stringify({ samples: sum, histogram: mapToArr(m) }));

/** Add v occurrences of k to a histogram map.
 * 
 * @param m histogram map: k -> occurrence count of k
 * @param k key
 * @param v new occurrences of k to add
 */
function histAdd(m, k, v) {
  var n = m.get(k);
  m.set(k, n ? n + v : v);
}

function mapToArr(m) {
  var a = [];
  for (e of m) a.push(e);
  return a;
} 
