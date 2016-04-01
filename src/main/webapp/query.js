function debug() {
  console.log(arguments)
}

// Address formats: although anything goes, Australian addresses often end with: Locality, State, PostCode.
// Last two should be pretty easy to find.

/**
 * Find postCode as last 4 digit word with no subsequent digits. returns { str: `s with postCode removed`, postCode: 1234 ], or if postCode not found { str: s, postCode: null }.
 */
function extractPostcode(s) {
  var re = /\b(\d{4})\b[^\d]*$/;
  debug('extractPostcode: s =', s);
  var idx = s.search(re);
  return idx === -1 ? { str: s, postCode: null } : { str: (s.slice(0, idx) + s.slice(idx + 4)).trim(), postCode: s.slice(idx, idx + 4) };
}

/**
 * Find last state. returns { str: `s with state removed`, state: 'ACT' ], or if state not found { str: s, state: null }.
 */
function extractState(s) {
  var re = /\b(AUSTRALIAN\s+CAPITAL\s+TERRITORY|ACT|NEW\s+SOUTH\s+WALES|NSW|NORTHERN\s+TERRITORY|NT|OTHER\s+TERRITORIES|OT|QUEENSLAND|QLD|SOUTH\s+AUSTRALIA|SA|TASMANIA|TAS|VICTORIA|VIC|WESTERN\s+AUSTRALIA|WA)\b/ig;
  var arr = null;
  var x;
  while (null !== (x = re.exec(s))) arr = x;
  return arr === null ? { str: s, state: null } : { str: (s.slice(0, arr.index) + s.slice(arr.index + arr[0].length)).trim(), state: arr[0] }
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
    : { str: (s.slice(0, x.index) + s.slice(x.index + x[0].length)).trim(), flatNumberFirst: x[1], flatNumberLast: x[2] === undefined ? null : x[2] };
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
   s = (s.slice(0, a.idx) + s.slice(a.idx + a.len)).trim();
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
  // debug('searchQuery: query = ', query, 'heuristics = ', heuristics);
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
      function addNumbers(nums) {
        nums.forEach( n =>
          terms.push( { multi_match: { 
            query: n, 
            fields: [ 'level.number^0.2', 'flat.number^0.4', 'numberFirst.number^0.5', 'numberLast.number^0.3', 'postcode^0.5' ],
            type: 'most_fields'
          } } )
        );
      }
      var idx = q5.numbers.findIndex(a => a.last !== null && a.first < a.last);
      if (idx === -1) {
        var num = q5.numbers[q5.numbers.length - 1];
        terms.push({ term: { 'numberFirst.number': num.first }});
        // finally search for all numbers (including what we think is 'numberFirst.number' above) in all numeric fields
        addNumbers(q5.numbers.map(a => a.first));
      } else {
        var num = q5.numbers[idx];
        terms.push({ term: { 'numberFirst.number': num.first }});
        terms.push({ term: { 'numberLast.number': num.last }});
        // finally search for all numbers (including what we think is 'numberFirst.number' & 'numberLast.number' above) in all numeric fields
        var nums = q5.numbers.map(a => a.first);
        nums.push(num.last);
        addNumbers(nums);
      }
    }
    // interesting test case: Unit 2, 94-96 Parker Street Templestowe 3106 3106 Templestowe
    // address is actually in: TEMPLESTOWE LOWER, 3107
    // but this scores higher: 94-96[A] SMITHS ROAD       TEMPLESTOWE     3106
    // Additional 'street.name' term just adds: 94 TEMPLESTOWE ROAD in 5th place.
    if (q5.str.trim().length > 0) {
      // terms.push( { match: { 'street.name': { query: q5.str, fuzziness: 1, prefix_length: 2, boost: 2.0 } } } );
      terms.push( { match: { '_all': { query: q5.str, fuzziness: 1, prefix_length: 2 } } } );
    }
    
  } else {
    terms.push( { match: { '_all': { query: query, fuzziness: 1, prefix_length: 2 } } } );
  }
  
  return { query: { bool: { should: terms, minimum_should_match: '75%' } } };
}

function preNumSuf(s) {
  debug('preNumSuf: s =', s);
  var re = /^([A-Z0-9]{0,3}?)(\d+)([A-Z0-9]{0,2}?)$/g; // developer.mozilla.org says '{0' not allowed - 'positive integer'
  var x = re.exec(s);
  return x === null ? null : { prefix: x[1], number: x[2], suffix: x[3] };
}

/**
 * Generate a query from individual fields (values must all be uppercase to match GNAF).
 * @param fields
 */
function fieldQuery(fields) {
  debug('fieldQuery: fields = ', fields);
  
  var shouldTerms = [];
  var mustTerms = [];
  
  /**
   * append terms for prefix, number, suffix to `terms`
   */
  function addPreNumSuf(preNumSuf, field, terms) {
    function add(name, value, nullValue) {
      nullValue = typeof nullValue !== 'undefined' ? nullValue : 'D61_NULL'; // Chrome 46.0.2490.86 (64-bit) doesn't appear to support default values yet
      debug('addPreNumSuf.add: name =', name, 'value =', value);
      var term = {};
      term[field + '.' + name] = value ? value : nullValue;
      terms.push( { term: term } );
    };
    debug('addPreNumSuf: preNumSuf =', preNumSuf);
    if (preNumSuf) {
      add('prefix', preNumSuf.prefix);
      add('number', preNumSuf.number, -1);
      add('suffix', preNumSuf.suffix);
    };
  };
  
  var fuzzy = true; // doesn't seem any faster with false
  function addMatch(field, query, terms) {
    if (query) {
      var match = {};
      match[field] = fuzzy ? { query: query, fuzziness: 1, prefix_length: 2 } : query;
      terms.push( { match: match } );
    }
  };
  
  if (fields.site) {
    addMatch('d61SiteBuilding', fields.site, shouldTerms);
  } else {
    shouldTerms.push( { term: { 'd61SiteBuilding': 'D61_NULL' } } );
  }
  
  if (fields.level) {
    var level = fields.level;
    var re = /([A-Z]*\d+[A-Z]*)/g;
    var nums = re.exec(level);
    if (nums !== null) {
      addPreNumSuf(preNumSuf(nums[1]), 'level', shouldTerms);
      level = level.slice(0, nums.index) + level.slice(nums.index + nums[0].length);
    }
    addMatch('d61Level', level, shouldTerms);
  } else {
    shouldTerms.push( { term: { 'level.number': -1 } } );
  }
  
  if (fields.flat) {
    var flat = fields.flat;
    // there are some digits in prefix/suffix values, but lets not worry about them for now
    var re = /([A-Z]*\d+[A-Z]*)/g;
    var nums = re.exec(flat);
    if (nums !== null) {
      addPreNumSuf(preNumSuf(nums[1]), 'flat', shouldTerms);
      flat = (flat.slice(0, nums.index) + ' ' + flat.slice(nums.index + nums[0].length)).trim();
    }
    addMatch('d61Flat', flat, shouldTerms); // TODO: maybe not necessary
  } else {
    shouldTerms.push( { term: { 'flat.number': -1 } } );
  }
  
  if (fields.street) {
    var street = fields.street;
    // there are some digits in prefix/suffix values, but lets not worry about them for now
    var re = /([A-Z]*\d+[A-Z]*)(?:-([A-Z]*\d+[A-Z]*))?/g;
    var nums = re.exec(street);
    if (nums !== null) {
      addPreNumSuf(preNumSuf(nums[1]), 'numberFirst', shouldTerms);
      addPreNumSuf(nums[2] === undefined ? null : preNumSuf(nums[2]), 'numberLast', shouldTerms);
      street = (street.slice(0, nums.index) + street.slice(nums.index + nums[0].length)).trim();
    }
    addMatch('street.name', street, mustTerms);
    addMatch('d61Street', street, shouldTerms);
  }
  
  addMatch('d61Locality', fields.locality, shouldTerms);
  
  if (fields.postcode) {
    shouldTerms.push( { term: { 'postcode': fields.postcode } } );
  }
  
  if (fields.state) {
    if (fields.state.length <= 3) shouldTerms.push( { term: { 'stateAbbreviation': fields.state } } );
    else addMatch('stateName', fields.state, shouldTerms);
  }
  
  return { query: { bool: { must: mustTerms, should: shouldTerms, minimum_should_match: '75%' } } };
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


if (typeof exports !== 'undefined') {
  // Node.js: require('./query') provides access to:
  exports.extractPostcode = extractPostcode;
  exports.extractState = extractState;
  exports.extractFlat = extractFlat;
  exports.extractNumbers = extractNumbers;
  exports.searchQuery = searchQuery;
  exports.fieldQuery = fieldQuery;
  exports.replaceNulls = replaceNulls;
}

