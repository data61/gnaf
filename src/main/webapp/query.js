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
  // TODO: could build this from var states in index.js
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

function preNumSuf(s) {
  debug('preNumSuf: s =', s);
  var re = /^([A-Z0-9]{0,3}?)(\d+)([A-Z0-9]{0,2}?)$/g; // TODO: developer.mozilla.org says '{0' not allowed - 'positive integer'
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
      match[field] = fuzzy ? { query: query, fuzziness: 2, prefix_length: 2 } : query;
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

if (typeof exports !== 'undefined') {
  // Node.js: require('./query') provides access to:
  exports.extractPostcode = extractPostcode;
  exports.extractState = extractState;
  exports.extractFlat = extractFlat;
  exports.extractNumbers = extractNumbers;
  exports.searchQuery = searchQuery;
  exports.fieldQuery = fieldQuery;
}

