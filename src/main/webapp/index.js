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
    params.size = 10;
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

