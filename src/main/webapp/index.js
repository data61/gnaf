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
  
  var elem = $('#street');
  elem.autocomplete({
    minLength: 2,
    source: suggest,
    select: function(e, selected) {
      e.preventDefault();
      debug('street autocomplete: selected = ', selected);
      elem.val(selected.item.value);
      $('#locality').val(selected.item.payload.localityName);
      $('#postcode').val(selected.item.payload.postcode);
      $('#state').val(selected.item.payload.stateAbbreviation);
    }
  });
  elem.data("ui-autocomplete")._renderItem = function (ul, item) {
      debug('street autocomplete: item', item);
      return $("<li>")
        .append($('<a>').attr({ class: item.class}).append(item.label))
        .appendTo(ul);
    };  
}

var baseUrl = 'http://localhost:9200/gnaf/';

function initBaseUrl() {
//  baseUrl = window.location.protocol === 'file:'
//    ? 'localhost:9200/gnaf/'  // use this when page served from a local file during dev
//    : '/analytics/rest/v1.0'; // use this when page served from webapp
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
  fields.forEach(a => $('#' + a).val(""));
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

function suggest(req, resp) {
  try {
    var params = { street: {
      text: req.term.trim().toUpperCase(), 
      completion: {
        field: "d61SugStreet",
        size: 10
      }
    } };
    debug('suggest: params =', params);
    $.ajax({
      type: 'POST',
      url: baseUrl + '_suggest',
      data: JSON.stringify(params),
      dataType: 'json',
      success: function(data, textStatus, jqXHR) {
        debug('suggest success: data', data, 'textStatus', textStatus, 'jqXHR', jqXHR);
        resp(data.street[0].options.map(i => ({ label: i.text + ', ' + i.payload.localityName + ', ' + i.payload.stateAbbreviation + ' ' + i.payload.postcode, value: i.text, payload: i.payload }) ));
      },
      error: function(jqXHR, textStatus, errorThrown) {
        debug('search ajax error jqXHR =', jqXHR, 'textStatus =', textStatus, 'errorThrown =', errorThrown);
        resp([]);
      }
    });
  } catch (e) {
    debug('suggest error: e =', e);
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
          elem.empty();
          elem.append(searchResult(data));
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
    new Col('Flat', 'record', flatColHandler),
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

