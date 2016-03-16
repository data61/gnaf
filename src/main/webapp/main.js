// Run: node main.js
// Reads purported address from stdin, write top GNAF match to stdout.

var request = require('request');
var readline = require('readline');
var query = require('./query');

function topHit(q) {
  q.size = 1;
  // console.log('q', JSON.stringify(q));
  request.post(
      'http://localhost:9200/gnaf/_search',
      { json: q },
      function (error, response, data) {
        if (!error && response.statusCode == 200) {
          var hits = data.hits.hits.map(h => {
            var obj = query.replaceNulls(h._source);
            obj.score = h._score;
            return obj;
          });
          console.log(JSON.stringify(hits[0]));
        } else {
          console.log('error', error, 'response', response);
        }
      }
  );
}

var rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
  terminal: false
});

rl.on('line', function(line) {
  topHit(query.searchQuery(line, true));
});