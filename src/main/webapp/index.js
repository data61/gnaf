$(document).ready(init);

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

function init() {
  initBaseUrl();
  $('#state').append(states.map(s => {
    var opt = $('<option>').val(s.code);
    opt.text(s.name);
    return opt;
  }));
  $('#searchForm button').on('click', stopProp(search));
  $('#clearFreeText').on('click', stopProp(clearFreeText));
  $('#clearFields').on('click', stopProp(clearFields));
  initSuggestAddress();
  initSuggestStreet();    
}

var baseUrl;

function initBaseUrl() {
  baseUrl = window.location.protocol === 'file:'
    ? 'http://localhost:9200/gnaf/'                                               // use this when page served from a local file during dev
    : window.location.protocol + '//' + window.location.hostname + ':9200/gnaf/'; // or this when page served from web server
}

function stopProp(f) {
  return function(ev) {
    ev.stopPropagation();
    f();
  };
}

function clearFreeText() {
  $('#freeText').val("");
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
      var a = replaceNulls(selected.item.payload);
      $('#site').val(siteColHandler(a));
      $('#level').val(levelColHandler(a));
      $('#flat').val(flatColHandler(a));
      $('#street').val(a.d61SugStreet.input);
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

function suggestAddress(req, resp) {
  try {
    var params = {
      "query": { "match": { "d61Address": { "query": req.term,  "fuzziness": 2, "prefix_length": 2 } } },
      "size": 10
    };
    debug('suggestAddress: params =', params);
    $.ajax({
      type: 'POST',
      url: baseUrl + '_search',
      data: JSON.stringify(params),
      dataType: 'json',
      success: function(data, textStatus, jqXHR) {
        debug('suggestAddress success: data', data, 'textStatus', textStatus, 'jqXHR', jqXHR);
        resp(data.hits.hits.map(i => ({ label: i._source.d61Address, payload: i._source }) ));
      },
      error: function(jqXHR, textStatus, errorThrown) {
        debug('suggestAddress ajax error jqXHR =', jqXHR, 'textStatus =', textStatus, 'errorThrown =', errorThrown);
        resp([]);
      }
    });
  } catch (e) {
    debug('suggestAddress error: e =', e);
  }
}

function initSuggestStreet() {
  var elem = $('#street');
  elem.autocomplete({
    minLength: 4,
    source: suggestStreet,
    select: function(e, selected) {
      e.preventDefault();
      debug('street autocomplete: selected =', selected);
      elem.val(selected.item.value);
      var a = selected.item.payload;
      $('#locality').val(a.localityName);
      $('#postcode').val(a.postcode);
      $('#state').val(a.stateAbbreviation);
    }
  });
  elem.data("ui-autocomplete")._renderItem = function (ul, item) {
    debug('street autocomplete: item', item);
    return $("<li>")
      .append($('<a>').append(item.label))
      .appendTo(ul);
  };  
}

function suggestStreet(req, resp) {
  try {
    var params = { street: {
      text: req.term.trim(), // .toUpperCase(), 
      completion: {
        field: "d61SugStreet",
        size: 10
      }
    } };
    debug('suggestStreet: params =', params);
    $.ajax({
      type: 'POST',
      url: baseUrl + '_suggest',
      data: JSON.stringify(params),
      dataType: 'json',
      success: function(data, textStatus, jqXHR) {
        debug('suggestStreet success: data', data, 'textStatus', textStatus, 'jqXHR', jqXHR);
        resp(data.street[0].options.map(i => ({ label: i.text + ', ' + i.payload.localityName + ', ' + i.payload.stateAbbreviation + ' ' + i.payload.postcode, value: i.text, payload: i.payload }) ));
      },
      error: function(jqXHR, textStatus, errorThrown) {
        debug('suggestStreet ajax error jqXHR =', jqXHR, 'textStatus =', textStatus, 'errorThrown =', errorThrown);
        resp([]);
      }
    });
  } catch (e) {
    debug('suggestStreet error: e =', e);
  }
}

function search() {
  var elem = $('#searchResult');
  try {
    elem.empty();
    addSpinner(elem);
    var freeText = $('#freeText').val().trim();
    var params = freeText.length > 0 ? searchQuery(freeText, $('#heuristics').is(':checked')) : fieldQuery(getFields());
    params.size = 10;
    debug('search: params =', params, 'stringified =', JSON.stringify(params));
    $.ajax({
      type: 'POST',
      url: baseUrl + '_search',
      data: JSON.stringify(params),
      dataType: 'json',
      success: function(data, textStatus, jqXHR) {
        try {
          debug('search success: data =', data, 'textStatus =', textStatus, 'jqXHR =', jqXHR);
          elem.empty().append(searchResult(data));
        } catch (e) {
          debug('search success error: e = ', e);
          elem.empty();
          showError(elem, e.message);
        }
      },
      error: function(jqXHR, textStatus, errorThrown) {
        debug('search ajax error jqXHR =', jqXHR, 'textStatus =', textStatus, 'errorThrown =', errorThrown);
        elem.empty();
        showError(elem, errorThrown);
      }
    });
  } catch (e) {
    debug('search error: e =', e);
    elem.empty();
    showError(elem, e.message);
  }
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
    new Col('Level', 'record', levelColHandler),
    new Col('Flat', 'record', flatColHandler),
    new Col('Street', 'd61SugStreet', streetColHandler),
    new Col('Locality', 'localityName'),
    new Col('Postcode', 'postcode'),
    new Col('State', 'stateName'),
    new Col('Location', 'location', locationColHandler)
  ]);
  return [ stats, table ];
}

function siteColHandler(h) {
  return filterJoin([ h.addressSiteName, h.buildingName ], ' ');
}

function levelColHandler(h) {
  return namePreNumSuf(h.levelName, h.level);
}

function flatColHandler(h) {
  return namePreNumSuf(h.flatTypeName, h.flat);
}

function streetColHandler(l) {
  return l.input;
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

