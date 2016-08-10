var readline = require('readline');

var d61Num = a => [ a.prefix, a.number.toString(), a.suffix ].filter(a => a != null && a != "D61_NULL" && a != "-1").join("");

var d61NumLast = a => a.number == -1 ? "" : '-' + d61Num(a);

var d61StreetNum = a => a.numberFirst.number == -1 ? "" : d61Num(a.numberFirst) + d61NumLast(a.numberLast);

/** Each inner array gets indexed as a separate Lucene "value" in the "d61Address" field.
 *  Although Lucene just concatenates all the values into the field there is a big position increment between the values
 *  ("position_increment_gap": 100 set in gnafMapping.json) that stops phrase searches and shingles (n-grams) matching across values.
 */
var d61Address = a =>
  [
    [ a.addressSiteName, a.buildingName ],
    [ a.flatTypeName, d61Num(a.flat) ], 
    [ a.levelTypeName, d61Num(a.level) ],
    [ d61StreetNum(a), a.street.name, a.street.typeCode, a.street.suffixName ],
    [ a.localityName, a.stateAbbreviation, a.postcode ]
  ].concat(
    a.streetVariant.map( x => [ x.name, x.typeCode, x.suffixName ]), 
    a.localityVariant.map( x => [ x.localityName, a.stateAbbreviation, a.postcode ])
  ).map(x => x.filter(x => x != "" && x != null && x != "D61_NULL").join(" ")).filter(x => x != "");

var rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
  terminal: false
});

rl.on('line', function (l) {
  var a = JSON.parse(l);
  a["d61Address"] = d61Address(a);
  console.log(
    JSON.stringify({ index: { _index: "gnaf", _type: "gnaf", _id: a.addressDetailPid } }) // one line of elasticsearch indexing metadata
    + '\n' + JSON.stringify(a) // next line is document to index
  );
});