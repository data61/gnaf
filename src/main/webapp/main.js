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
          var h = data.hits.hits[0];
          var obj = query.replaceNulls(h._source);
          obj.score = h._score;
          console.log(JSON.stringify(obj));
        } else {
          console.log('error', error, 'response', response);
        }
      }
  );
}

function getFields(line) {
  var fields = {};
  fields.site = fields.level = fields.flat = null; 
  
  // 0                  1               2               3               4
  // rcp_addr_line0     rcp_addr_line1  rcp_loc_code    rcp_loc_name    rcp_loc_code_class
  var arr = line.split('~');
  // console.log('getFields: line =', line, 'arr =', arr);
  
  var pc = query.extractPostcode(arr[2]);
  fields.postcode = pc.postCode
  var loc = query.extractPostcode(arr[3]);
  var st = query.extractState(loc.str);
  fields.state = st.state;
  fields.locality = st.str;
  
  var re = /\d/;
  var ad = query.extractFlat(re.test(arr[0]) ? arr[0] : arr[1]);
  fields.street = ad.str;
  // console.log('getFields: line =', line, 'fields =', fields);
  return fields;
}

var rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
//  completer: function(linePartial, callback) { // attempt to stop tabs being eaten! (should work in newer versions)
//    callback(null, [[], linePartial]);
//  },
  terminal: false
});

rl.on('line', function(line) {
  var f = getFields(line.toUpperCase());
  topHit(query.fieldQuery(f));
});

//rl.on('close', () => {
//  console.log('all done');
//  process.exit(0);
//});