// Standalone test script. Run: node test.js

var query = require('./query');

var data = [
         'UNIT 1215 NO 700-710',
         'Unit 7 35 - 39',
         'UNIT 4-6 / 246',
         '26-30',
         '16-18/424-426',
         '18/424-426',
         '33 EDWARD ST-UNIT6',
         'FLAT 18C  131 CURRUMBURRA ROAD'
         ];

data.forEach(d => {
  var flat = query.extractFlat(d);          // take flat number first
  var num = query.extractNumbers(flat.str); // then take any remaining numbers
  console.log('input', d, 'flat', JSON.stringify(flat), 'num', JSON.stringify(num)); // num.str is everything left
});
