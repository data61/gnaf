
var states = [
  { code: 'ACT', name: 'AUSTRALIAN CAPITAL TERRITORY' },
  { code: 'NSW', name: 'NEW SOUTH WALES' },
  { code: 'NT', name: 'NORTHERN TERRITORY' },
  { code: 'OT', name: 'OTHER TERRITORIES' },
  { code: 'QLD', name: 'QUEENSLAND' },
  { code: 'SA', name: 'SOUTH AUSTRALIA' },
  { code: 'TAS', name: 'TASMANIA' },
  { code: 'VIC', name: 'VICTORIA' },
  { code: 'WA', name: 'WESTERN AUSTRALIA' }
];

function initSearch() {
  initBaseUrl();
  $('#state').append(states.map(s => {
    var opt = $('<option>').val(s.code);
    opt.text(s.name);
    return opt;
  }));
  $('#searchForm button').on('click', stopPropagation(search));
  $('#clearFreeText').on('click', stopPropagation(clearFreeText));
  $('#clearFields').on('click', stopPropagation(clearFields));
  initSuggestAddress();
}

function initBulk() {
  initBaseUrl();
  $('#searchForm button').on('click', stopPropagation(bulk));
  $('#clearFreeText').on('click', stopPropagation(clearFreeText));
}

var baseUrl;

function initBaseUrl() {
  baseUrl = window.location.protocol === 'file:'
    ? 'http://gnaf.it.csiro.au/es/' // 'http://localhost:9200/gnaf/' // use this when page served from a local file during dev
    // : window.location.protocol + '//' + window.location.hostname + ':9200/gnaf/'; // or this when page served from web server
    : window.location.protocol + '//' + window.location.hostname + '/es/'; // or this when page served from web server
}

function stopPropagation(f) {
  return function(ev) {
    ev.stopPropagation();
    f();
  };
}

function clearFreeText() {
  $('#freeText').val('');
}

var fields = [ 'site', 'level', 'flat', 'street', 'locality', 'postcode', 'state' ];

function clearFields() {
  fields.forEach(a => $('#' + a).val(a === 'state' ? 'ACT' : ''));
}

/**
 * @returns object with a key named after each element in `fields` with trimmed, uppercased value taken from the DOM element with that id. 
 */
function getFields() {
  return fields.reduce(
    (obj, f) => {
      obj[f] = $('#' + f).val().trim().toUpperCase();
      return obj;
    },
    ({})
  );
}

function filterJoin(arr, sep) {
  return arr.filter(x => x !== null && x !== '').join(sep)
}

function namePreNumSuf(name, n) {
  return filterJoin([name, filterJoin([n.prefix, n.number, n.suffix], '') ], ' ');
}

function initSuggestAddress() {
  var elem = $('#freeText');
  elem.autocomplete({
    minLength: 4,
    source: suggestAddress,
    select: function(e, selected) {
      e.preventDefault();
      debug('address autocomplete: selected =', selected, 'value =', selected.item.value);
      elem.val(selected.item.value);
      var a = selected.item.payload;
      $('#site').val(siteColHandler(a));
      $('#level').val(levelColHandler(a));
      $('#flat').val(flatColHandler(a));
      $('#street').val(streetColHandler(a));
      $('#locality').val(a.localityName);
      $('#postcode').val(a.postcode);
      $('#state').val(a.stateAbbreviation);
      $('#searchResult').empty().append(searchResult({ took: 0, hits: { max_score: 1.0, total: 1, hits: [ { _score: 1.0, _source: a } ] } }));
    }
  });
  elem.data("ui-autocomplete")._renderItem = function (ul, item) {
    debug('address autocomplete: item', item);
    return $("<li>")
      .append($('<a>').append(item.label))
      .appendTo(ul);
  };
}

function bulk() {
  var r = $('#searchResult');
  var tbl = genTable([], searchResultCols);
  r.empty().append(tbl);
  
  function q(terms) {
    runQuery(
      d61AddressQuery(terms, 1),
      function(data) {
        data.hits.max_score = 1.0;
        tbl.append(genRow(dataHits(data)[0], searchResultCols));
      },
      function() {}
    );
  }
  
  $('#freeText').val().split('\n').forEach(addr => q(addr));
}

function suggestAddress(req, resp) {
  runQuery(
    d61AddressQuery(req.term, 10),
    function(data) {
      resp(data.hits.hits.map(i => ({ label: hitToAddress(i._source), payload: i._source }) ));
    },
    function() {
      resp([]);
    }
  );
}

/**
 * Instead of "UNIT 2 12 BLAH STREET" people often use "2 / 12 BLAH STREET".
 * If the shingle filter is used to store 2 & 3-grams "2 12 BLAH" will get a high score, but "2 / 12 BLAH" won't;
 * so we replace the "/" with " ".
 * @param addr
 * @returns
 */
function flatSeparator(addr) {
  return addr.replace(/\//g, ' ');
}

function d61AddressQuery(terms, size) {
  return {
    "query": { "match": { "d61Address": { "query": flatSeparator(terms),  "fuzziness": 2, "prefix_length": 2 } } },
    "size": size
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

function runQuery(params, success, error) {
  try {
    debug('runQuery: params =', params);
    $.ajax({
      type: 'POST',
      url: baseUrl + '_search',
      data: JSON.stringify(params),
      contentType: "application/json; charset=utf-8",
      dataType: 'json',
      success: function(data, textStatus, jqXHR) {
        try {
          debug('runQuery success: data', data, 'textStatus', textStatus, 'jqXHR', jqXHR);
          data.hits.hits.forEach(h => replaceNulls(h));
          success(data);
        } catch (e) {
          debug('runQuery success error: e = ', e);
          error(e.message);
        }
      },
      error: function(jqXHR, textStatus, errorThrown) {
        debug('runQuery ajax error jqXHR =', jqXHR, 'textStatus =', textStatus, 'errorThrown =', errorThrown);
        error(errorThrown);
      }
    });
  } catch (e) {
    debug('runQuery error: e =', e);
    error(e.message);
  }
}

function search() {
  var elem = $('#searchResult');
  elem.empty();
  addSpinner(elem);
  var freeText = $('#freeText').val().trim();
  runQuery(
    freeText.length > 0 ? d61AddressQuery(freeText, 10) : fieldQuery(getFields()),
    function(data, textStatus, jqXHR) {
      elem.empty().append(searchResult(data));
    },
    function(msg) {
      elem.empty();
      showError(elem, msg);
    }
  );
}

var searchResultCols = [
  new Col('Rank', 'score', scoreColHandler),
  new Col('Site', 'record', siteColHandler),
  new Col('Flat', 'record', flatColHandler),
  new Col('Level', 'record', levelColHandler),
  new Col('Street', 'record', streetColHandler),
  new Col('Locality', 'localityName'),
  new Col('Postcode', 'postcode'),
  new Col('State', 'stateName'),
  new Col('Location', 'location', locationColHandler)
];

function dataHits(data) {
  return data.hits.hits.map(h => {
    var obj = h._source;
    obj.score = h._score / data.hits.max_score;
    obj.record = obj; // for colHandler to access whole record
    return obj;
  });
}

function searchResult(data) {
  var stats = $('<span>').attr('class', 'stats').text(data.hits.hits.length.toLocaleString() + ' of ' + data.hits.total.toLocaleString() + ' hits in ' + (data.took/1000.0).toFixed(3) + ' sec');
  var table = genTable(dataHits(data), searchResultCols);
  return [ stats, table ];
}

function siteColHandler(h) {
  return filterJoin([ h.addressSiteName, h.buildingName ], ' ');
}

function flatColHandler(h) {
  return namePreNumSuf(h.flatTypeName, h.flat);
}

function levelColHandler(h) {
  return namePreNumSuf(h.levelName, h.level);
}

function streetNum(h) {
  var f = namePreNumSuf(null, h.numberFirst);
  var l = namePreNumSuf(null, h.numberLast);
  return l === '' ? f : f + '-' + l;
}

function streetColHandler(h) {
  return filterJoin([ streetNum(h), h.street.name, h.street.typeCode, h.street.suffixName ], ' ');
}

function locationColHandler(l) {
  return l.lat + ', ' + l.lon;
}

function hitToAddress(h) {
  return filterJoin([
    siteColHandler(h),
    filterJoin([ flatColHandler(h), levelColHandler(h), streetColHandler(h) ], ' '),
    filterJoin([ h.localityName, h.stateAbbreviation, h.postcode ], ' ')
  ], ', ');
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
    t.append(genRow(x, cols));
  });
  return t;
}

function genRow(x, cols) {
  var tr = $('<tr>');
  for (var i = 0; i < cols.length; i++) {
    var col = cols[i];
    tr.append($('<td>').attr('class', col.tdClass).append(col.handler(x[col.field])));
  }
  return tr;
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

