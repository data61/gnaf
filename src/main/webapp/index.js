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
// Last two should be pretty easy to find.

/* This data was used to test the extract*() functions with node.js:
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
 */


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
  query = query.toUpperCase();
  debug('searchQuery: query = ', query, 'heuristics = ', heuristics);
  var terms =
    [
     { constant_score: { 
       filter: { term: { 'primarySecondary': '0' } }, 
       boost: 0.8 
     }}, // no units/flats at this street number
     { constant_score: { 
       filter: { term: { 'primarySecondary': 'P' } }, 
       boost: 1.0 
     }}, // primary address for whole block of units/flats, else 'S'
     // for a unit/flat
     { constant_score: { 
       filter: { term: { 'aliasPrincipal':   'P' } }, 
       boost: 1.0 
     }}, // principal address preferred over an alias
     { match: { 'd61NullStr': 'D61_NULL' }}, // faster than alternative below, seems to work OK
//     { multi_match: { 
//       query: 'D61_NULL',
//       type:  'most_fields',
//       fields: [ 
//         'levelTypeCode', 'level.prefix', 'level.suffix', 
//         'flatTypeCode', 'flat.prefix', 'flat.suffix',
//         'numberFirst.prefix', 'numberFirst.suffix',
//         'numberLast.prefix', 'numberLast.suffix',
//         'street.suffixCode'
//       ] 
//     } }, // null replacement value preferred over any value we did not search for - 'simplest' first, works well but slow!
     { match: { 'd61NullInt': '-1' }} // faster than alternative below, seems to work OK
//     { multi_match: { 
//       query: '-1', 
//       type:  'most_fields',
//       fields: [ 'level.number', 'flat.number', 'numberLast.number' ]
//     } } // null replacement value preferred over any value we did not search for - 'simplest' first
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
        // finally search for all numbers (including what we think is 'numberFirst.number' above) in all numeric fields
        terms.push( { multi_match: {
          query: q5.numbers.map(a => a.first).join(' '), 
          fields: [ 'level.number^0.2', 'flat.number^0.4', 'numberFirst.number^0.5', 'numberLast.number^0.3', 'postcode^0.5' ]
        } } ); 
      } else {
        var num = q5.numbers[idx];
        terms.push({ term: { 'numberFirst.number': num.first }});
        terms.push({ term: { 'numberLast.number': num.last }});
        // finally search for all numbers (including what we think is 'numberFirst.number' & 'numberLast.number' above) in all numeric fields
        terms.push( { multi_match: { 
          query: q5.numbers.map(a => a.first).push(num.last).join(' '), 
          fields: [ 'level.number^0.2', 'flat.number^0.4', 'numberFirst.number^0.5', 'numberLast.number^0.3', 'postcode^0.5' ]
        } } ); 
      }
    }
    if (q5.str.trim().length > 0) terms.push( { multi_match: { 
      query: q5.str, fuzziness: 1, prefix_length: 2, fields: [ '_all^0.5', 'street.name^2.0']
    } } );
    
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
    if (obj === 'D61_NULL' || obj === -1) h[p] = null;
    else if (typeof obj === 'object' && !Array.isArray(obj)) replaceNulls(obj);
  }
  return h;
}

function searchResult(data) {
  var stats = $('<span>').attr('class', 'stats').text(data.hits.hits.length.toLocaleString() + ' of ' + data.hits.total.toLocaleString() + ' hits in ' + (data.took/1000.0).toFixed(3) + ' sec');
  var hits = data.hits.hits.map(h => {
    var obj = replaceNulls(h._source);
    obj.score = h._score;
    obj.record = obj; // for colHandler to access whole record
    return obj;
  });
  var table = genTable(hits, [
    new Col('Rank', 'score', scoreColHandler),
    new Col('Site', 'record', siteColHandler),
    new Col('Unit', 'record', flatColHandler),
    new Col('Level', 'record', levelColHandler),
    new Col('Street', 'record', streetColHandler),
    new Col('Locality', 'localityName'),
    new Col('Postcode', 'postcode'),
    new Col('State', 'stateName'),
    new Col('Location', 'location', locationColHandler)
  ]);
  return [ stats, table ];
}

function siteColHandler(h) {
  return [ h.addressSiteName, h.buildingName ].join(' ');
}

function flatColHandler(h) {
  return [ h.flatTypeCode, h.flat.prefix, h.flat.number, h.flat.suffix ].join(' ');
}

function levelColHandler(h) {
  return [ h.levelName, h.level.prefix, h.level.number, h.level.suffix ].join(' ');
}

function streetColHandler(h) {
  var first = [ h.numberFirst.prefix, h.numberFirst.number, h.numberFirst.suffix ].join('').trim();
  var last = [ h.numberLast.prefix, h.numberLast.number, h.numberLast.suffix ].join('').trim();
  var range = first.length > 0 && last.length > 0 ? first + '-' + last : first + last; 
  return [ range, h.street.name, h.street.typeCode, h.street.suffixCode ].join(' ');
}

function locationColHandler(l) {
  return l.lat + ', ' + l.lon;
}

// *********************************************************
// building tables:

/**
 * Generate Table.
 * <p>
 * @param data for table
 * @param cols array of (column header text, attribute of data row to pass to handler, handler to generate cell content)
 * @return table element
 */
function genTable(data, cols) {
  var t = $('<table>');
  var tr = $('<tr>');
  // column headers from 'labels'
  t.append(tr);
  for (var i = 0; i < cols.length; i++) {
    var col = cols[i];
    tr.append($('<th>').attr('class', col.tdClass).text(cols[i].label));
  }
  // make a row from each element in 'data'
  // 'fields' gives the properties to use and their order
  $.each(data, function(index, x) {
    var tr = $('<tr>');
    for (var i = 0; i < cols.length; i++) {
      var col = cols[i];
      tr.append($('<td>').attr('class', col.tdClass).append(col.handler(x[col.field])));
    }
    t.append(tr);
  });
  return t;
}

// what genTable needs to know for each column
function Col(label, field, handler, tdClass) {
  this.label = label; // label for header
  this.field = field; // name of field for data
  this.handler = typeof handler !== 'undefined' ? handler : defaultColHandler; // a function mapping data item => content of table/tr/td
  this.tdClass = typeof tdClass !== 'undefined' ? tdClass : field;
}

// functions you can use for the above Col.handler
function defaultColHandler(v) { return v; }
function scoreColHandler(v) { return v === "NaN" ? "" : v.toPrecision(2); }

//*********************************************************
// debug and error logging:

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

