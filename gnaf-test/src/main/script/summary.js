var fs = require('fs');

/**
 * Usage: node src/main/node/summary.js files ...
 */
var m = new Map();
for (i = 2; i < process.argv.length; ++i) { // 0 -> node; 1 -> src/main/script/summary.js; 2 -> file1 ...
  var stats = JSON.parse(fs.readFileSync(process.argv[i], "utf8"));
  for (desc in stats.histogram) {
    if (desc != 'nofuzTypo') { // skip because we cannot match types without fuz
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
