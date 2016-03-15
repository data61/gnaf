$(document).ready(init);

function init() {
  // initBaseUrl();
  $('#searchForm button').on('click', search);
}

var baseUrl = 'http://localhost:9200/gnaf/';

// function initBaseUrl() {
// baseUrl = window.location.protocol === 'file:'
// ? 'localhost:9200/gnaf/' // use this when page served from a local file
// during dev
// : '/analytics/rest/v1.0'; // use this when page served from webapp
// }

function search() {
  var url = baseUrl + '_search';
  var elem = $('#searchResult');
  try {
    elem.empty();
    addSpinner(elem);
    var params = searchQuery($('#query').val(), $('#heuristics').is(':checked'));
    debug('search:', 'url', url, 'params', params, 'stringified', JSON.stringify(params));
    $.ajax({
      type: 'POST',
      url: url,
      data: JSON.stringify(params),
      dataType: 'json',
      success: function(data, textStatus, jqXHR) {
        try {
          debug('search success: data', data, 'textStatus', textStatus, 'jqXHR', jqXHR);
          elem.empty();
          elem.append(searchResult(data));
        } catch (e) {
          debug('search success error: e = ', e);
          elem.empty();
          showError(elem, e.message);
        }
      },
      error: function(jqXHR, textStatus, errorThrown) {
        debug('search ajax error jqXHR = ', jqXHR, 'textStatus = ', textStatus, 'errorThrown = ', errorThrown);
        elem.empty();
        showError(elem, errorThrown);
      }
    });
  } catch (e) {
    debug('search error: url = ' + url, e);
    elem.empty();
    showError(elem, e.message);
  }
}

// Address formats: although anything goes, Australian addresses often end with: Locality, State, PostCode.
// Last two should be pretty easy to find

/**
 * Find postCode as last 4 digit word with no subsequent digits. returns { str: `s with postCode removed`, postCode: 1234 ], or if postCode not found { str: s, postCode: null }.
 */
function extractPostcode(s) {
  var re = /\b(\d{4})\b[^\d]*$/;
  var idx = s.search(re);
  return idx === -1 ? { str: s, postCode: null } : { str: s.slice(0, idx) + s.slice(idx + 4), postCode: s.slice(idx, idx + 4) };
}

/**
 * Find last state. returns { str: `s with state removed`, state: 'ACT' ], or if state not found { str: s, state: null }.
 */
function extractState(s) {
  var re = /\b(AUSTRALIAN\s+CAPITAL\s+TERRITORY|ACT|NEW\s+SOUTH\s+WALES|NSW|NORTHERN\s+TERRITORY|NT|OTHER\s+TERRITORIES|OT|QUEENSLAND|QLD|SOUTH\s+AUSTRALIA|SA|TASMANIA|TAS|VICTORIA|VIC|WESTERN\s+AUSTRALIA|WA)\b/ig;
  var arr = null;
  var x;
  while (null !== (x = re.exec(s))) arr = x;
  return arr === null ? { str: s, state: null } : { str: s.slice(0, arr.index) + s.slice(arr.index + arr[0].length), state: arr[0] }
}

/**
 * Examples in the wild:
 */
var d = [
  'UNIT 1215 NO 700-710',
  'Unit 7 35 - 39',
  'UNIT 4-6 / 246',
  '26-30',
  '16-18/424-426',
  '18/424-426',
  '33 EDWARD ST-UNIT6',
  'FLAT 18C  131 CURRUMBURRA ROAD'
  ];

/**
 * Find flat/unit number. returns { str: `s with flat/unit number removed`, flatNumberFirst: num, flatNumberLast: num }
 */
function extractFlat(s) {
  var re1 = /\b(?:UNIT|FLAT)\s*(\d+)(?:-(\d+))?/i; // 'UNIT 5' or 'UNIT 5 - 7'
  var re2 = /(\d+)(?:-(\d+))?\s*\//;               // '5 /' or '5 - 7 /'
  var x = re1.exec(s);
  if (x === null) x = re2.exec(s);
  return x === null ? { str: s, flatNumberFirst: null, flatNumberLast: null }
    : { str: s.slice(0, x.index) + s.slice(x.index + x[0].length), flatNumberFirst: x[1], flatNumberLast: x[2] === undefined ? null : x[2] };
}

/**
 * Find numbers and ranges like 5 - 7). returns { str: `s with numbers removed`, numbers: [ { first: 5, last: 7 }, ... ] }, or if numbers not found { str: s, numbers: [] }.
 */
function extractNumbers(s) {
 var re = /(\d+)(?:-(\d+))?/g;
 var arr = [];
 var x;
 while (null !== (x = re.exec(s))) arr.push( { idx: x.index, len: x[0].length, first: x[1], last: x[2] === undefined ? null : x[2] } );
 for (var i = arr.length - 1; i >= 0; --i) {
   var a = arr[i];
   s = s.slice(0, a.idx) + s.slice(a.idx + a.len);
 }
 return { str: s, numbers: arr.map(a => ({ first: a.first, last: a.last }) ) };
}

/**
 * Returns query body for Elasticsearch.
 * 
 * @param query
 *                string purporting to be an address
 * @param heuristics
 *                whether to attempt to extract state, postcode and other numbers from the query for a more targeted search
 */
function searchQuery(query, heuristics) {
  debug('searchQuery: query = ', query, 'heuristics = ', heuristics);
  var terms =
    [
     { constant_score: { 
       filter: { term: { 'primarySecondary': '0' } }, 
       boost: 1.0 
     }}, // no units/flats at this street number
     { constant_score: { 
       filter: { term: { 'primarySecondary': 'P' } }, 
       boost: 0.8 
     }}, // primary address for whole block of units/flats, else 'S'
     // for a unit/flat
     { constant_score: { 
       filter: { term: { 'aliasPrincipal':   'P' } }, 
       boost: 1.0 
     }}, // principal address preferred over an alias
//     { match: { '_all': 'D61_NULL' }}, // unfortunately this doesn't rank 'simplest' (least spurious fields) first
     // after re-indexing, try "d61NullStr"
     { multi_match: { 
       query: 'D61_NULL',
       type:  'most_fields',
       fields: [ 
         'levelTypeCode', 'level.prefix', 'level.suffix', 
         'flatTypeCode', 'flat.prefix', 'flat.suffix',
         'numberFirst.prefix', 'numberFirst.suffix',
         'numberLast.prefix', 'numberLast.suffix',
         'street.suffixCode'
       ] 
     } }, // null replacement value preferred over any value we did not search for - 'simplest' first, works well but slow!
     { multi_match: { 
       query: '-1', 
       type:  'most_fields',
       fields: [ 'level.number', 'flat.number', 'numberLast.number' ]
     } } // null replacement value preferred over any value we did not search for - 'simplest' first
     // after re-indexing, try "d61NullInt"
   ];
  if (heuristics) {
    var q2 = extractState(query);
    if (q2.state !== null) terms.push(
      q2.state.length <= 3 ? { term: { 'stateAbbreviation': q2.state } } : { match: { 'stateName': { query: q2.state } } }
    );
    var q3 = extractPostcode(q2.str);
    if (q3.postCode !== null) terms.push( { term: { 'postcode': q3.postCode } } );
    var q4 = extractFlat(q3.str);
    if (q4.flatNumberFirst !== null) terms.push( { term: { 'flat.number': q4.flatNumberFirst } } ); // flatNumberLast - not in GNAF
    var q5 = extractNumbers(q4.str);
    if (q5.numbers.length > 0) {
      var idx = q5.numbers.findIndex(a => a.last !== null && a.first < a.last);
      if (idx === -1) {
        var num = q5.numbers[q5.numbers.length - 1];
        terms.push({ term: { 'numberFirst.number': num.first }});
        // finally search for all numbers (including what we think is
        // 'numberFirst.number' above) in all numeric fields
        terms.push( { multi_match: {
          query: q5.numbers.map(a => a.first).join(' '), 
          fields: [ 'level.number^0.1', 'flat.number^0.2', 'numberFirst.number^0.5', 'numberLast.number^0.2', 'postcode' ]
        } } ); 
      } else {
        var num = q5.numbers[idx];
        terms.push({ term: { 'numberFirst.number': num.first }});
        terms.push({ term: { 'numberLast.number': num.last }});
        // finally search for all numbers (including what we think is
        // 'numberFirst.number' & 'numberLast.number' above) in all numeric
        // fields
        terms.push( { multi_match: { 
          query: q5.numbers.map(a => a.first).push(num.last).join(' '), 
          fields: [ 'level.number^0.1', 'flat.number^0.2', 'numberFirst.number^0.5', 'numberLast.number^0.2', 'postcode' ]
        } } ); 
      }
    }
    if (q5.str.trim().length > 0) terms.push( { match: { '_all': { query: q5.str, fuzziness: 1, prefix_length: 2 } } } );
    
  } else {
    terms.push( { match: { '_all': { query: query, fuzziness: 1, prefix_length: 2 } } } );
  }
  
  return {
    query: { bool: { should: terms } },
    size: 10
  };
}

/**
 * Replace values used to represent `searchable nulls` with actual nulls.
 * <p>
 * Elastic search omits nulls from the index, consequently they cannot be searched for.
 * The indexing process substitutes 'D61_NULL' for null values to make them searchable and here we do the reverse.
 * 
 * @param h
 */
function replaceNulls(h) {
  for (var p in h) {
    var obj = h[p];
    if (obj === 'D61_NULL') h[p] = null;
    else if (typeof obj === 'object' && !Array.isArray(obj)) replaceNulls(obj);
  }
  return h;
}

/*
 * {"took":49,"timed_out":false,"_shards":{"total":5,"successful":5,"failed":0},"hits":{"total":1214,"max_score":6.0664215,"hits":[{"_index":"gnaf","_type":"gnaf","_id":"GAACT717685921","_score":6.0664215,"_source":{ "addressDetailPid":"GAACT717685921","addressSiteName":null,"buildingName":null, "flatTypeCode":"UNIT","flat":{"prefix":null,"number":16,"suffix":null}, "levelName":null,"level":{"prefix":null,"number":null,"suffix":null}, "numberFirst":{"prefix":null,"number":61,"suffix":null},
 * "numberLast":{"prefix":null,"number":null,"suffix":null}, "street":{"name":"CURRONG","typeCode":"STREET","suffixCode":"N"}, "localityName":"BRADDON","stateAbbreviation":"ACT","stateName":"AUSTRALIAN CAPITAL TERRITORY","postcode":"2612", "location":{"lat":-35.27700812,"lon":149.13506014}, "streetVariant":[{"name":"CURRONG","typeCode":"STREET","suffixCode":null},{"name":"CURRONG STREET","typeCode":"NORTH","suffixCode":null}], "localityVariant":[{"localityName":"CANBERRA CENTRAL"},{"localityName":"CITY"}]}}
 */
function searchResult(data) {
  return data.hits.hits.map(hit => {
    var h = replaceNulls(hit._source);
    debug('searchResult: hit = ', hit);
    return $('<div>').text('score: ' + hit._score + '; ' + [
      h.addressSiteName, h.buildingName,
      h.flatTypeCode, h.flat.prefix, h.flat.number, h.flat.suffix,
      h.levelName, h.level.prefix, h.level.number, h.level.suffix,
      h.numberFirst.prefix, h.numberFirst.number, h.numberFirst.suffix, 
      h.numberLast.prefix, h.numberLast.number, h.numberLast.suffix, 
      h.street.name, h.street.typeCode, h.street.suffixCode,
      h.localityName, h.postcode, h.stateName
      ].join(' '));
  });
}

// debug and error logging

function debug() {
  console.log(arguments)
}

// add an .onEnter function we can use for input fields
// (function($) {
// $.fn.onEnter = function(func) {
// this.bind('keypress', function(e) {
// if (e.keyCode == 13) func.apply(this, [ e ]);
// });
// return this;
// };
// })(jQuery);

function addSpinner(elem) {
  elem.append('<div class="spinner"><img src="ajax-loader.gif" alt="spinner"></div>');
}

function showError(elem, error) {
  elem.append($('<div>').attr('class', 'error').text(error));
}

// 6 hits in 0.0250 sec
// function stats(data) {
// return $('<span>').attr('class', 'stats').text(data.totalHits + ' hits in ' +
// data.elapsedSecs.toFixed(3) + ' sec');
// }

