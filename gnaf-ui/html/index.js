function debug() {
  console.log(arguments)
}

var tabDef = [
  { id: 'addressLookup', tabText: 'Address Lookup', content: createAddressLookup }, 
  { id: 'addressesNearMe', tabText: 'Nearest Addresses', content: createAddressesNearMe }, 
  { id: 'bulk', tabText: 'Bulk Lookup', content: createBulkLookup }
];

function initGnaf() {
  initBaseUrl();
  $('#gnaf').append( [ createLocation(), createTabs('tabs', tabDef) ]);
  updateLocation();
}

var esUrl; // Elasticsearch
var dbUrl; // GNAF (database) Service

function initBaseUrl() {
  if (window.location.protocol === 'file:') {
    var host = 'http://gnaf.it.csiro.au';
    esUrl = host + '/es/'; // nginx proxy for CORS since Elastcsearch CORS appears broken
    dbUrl = host + ':9000/'; // CORS out of the box
  } else {
    var host = window.location.protocol + '//' + window.location.hostname;
    esUrl = host + ':9200/gnaf/';
    dbUrl = host + ':9000/';
  }
}

var myCoords;

var searchDistance = [ 50, 100, 500, 1000, 2000, 5000, 10000, 50000, 100000 ];

function createLocation() {
  return $('<div>').addClass('location').append([ 
    $('<label>').text('Location'), 
    $('<span>').attr('id', 'location'), 
    $('<a>').attr('href', '#').text('update').click(stopPropagation(updateLocation)),
    $('<br>'),
    $('<label>').text('Search distance'),
    $('<select>').attr('id', 'searchDistance')
      .append($('<option>').attr('value', -1).text("Don't use location"))
      .append(searchDistance.map(d => $('<option>').attr('value', d).text(d < 1000 ? d + 'm' : (d/1000) + 'km')))
  ]);
}

function updateLocation() {
  navigator.geolocation.getCurrentPosition(pos => {
    debug('updateLocation: coords =', pos.coords);
    $('#location').text(pos.coords.latitude.toString() + ',' + pos.coords.longitude.toString() + " ±" + pos.coords.accuracy.toString() + 'm');
    // * 3 because accuracy appears to be overly optimistic
    var nextHigherDist = searchDistance.find(d => d >= pos.coords.accuracy * 3);
    $('#searchDistance').val(nextHigherDist ? nextHigherDist : 5000);
    myCoords = pos.coords;
  });
}

function getDist() {
  var v = $('#searchDistance').val();
  return v == -1 ? null : v + 'm';
}

function createTabs(id, itc) {
  var tabs = $('<div>').attr('id', id);
  $('<ul>').append(
    itc.map(it => $('<li>').append($('<a>').attr('href', '#' + it.id).text(it.tabText)))
  ).appendTo(tabs);
  tabs.append(itc.map(it => $('<div>').attr('id', it.id).append(it.content())));
  tabs.tabs({
    activate: function(ev, ui) {
      debug('tabs.activate: ev =', ev, 'ui =', ui);
      var sel = '#' + tabDef[1].id;
      if (ui.newPanel.selector == sel) searchAddressesNearMe(myCoords, $('#addressNearMe'))
    } 
  });
  return tabs;
}


function createAddressLookup() {
  var id = 'addr';
  var lbl = $('<label>').attr('for', id).text('Enter address');
  var inp = $('<input>').attr({ id: id, name: id, type: 'text'});
  var result = $('<div>').attr('id', 'addressLookupResult');
  var btn = $('<button>').attr('type', 'button').text('Search').click(stopPropagation(search(inp, result)));
  initAutoCompleteAddress(inp, result);
  return [ lbl, inp, $('<br>'), btn, result ];
}

function createAddressesNearMe() {
  return $('<div>').attr('id', 'addressNearMe');
}

function createBulkLookup() {
  var id = 'bulkAddresses';
  var textarea = $('<textarea>').attr({id: id, name: id});
  var result = $('<div>').attr('id', 'bulkResult');
  return $('<div>').attr('id', 'bulkLookup').append([
    $('<div>').addClass('header').append([
      $('<label>').attr('for', id).text('Addresses'),
      $('<span>').addClass('example').text('one per line e.g.'),
      $('<span>').addClass('example').addClass('multi-line').append([
        $('<span>').text('7 London Circuit, ACT 2601'),
        $('<br>'),
        $('<span>').text('18 London Circuit, ACT 2601')
      ]),
      $('<a>').attr('href', '#').text('clear').click(stopPropagation(() => textarea.val('')))
    ]),
    textarea,
    $('<br>'),
    $('<button>').attr('type', 'button').text('Search').click(stopPropagation(() => bulkSearch(textarea.val().split('\n'), result))),
    result
  ]);
}

function bulkSearch(addresses, result) {
  var loc = toQueryLoc(myCoords);
  var dist = getDist();
  var queries = addresses.map(a => '{}\n' + JSON.stringify(d61AddressQuery(loc, dist, a, 1)) + '\n').join('');
  debug('bulkSearch: queries =', queries);
  doAjax(
    esUrl + '_msearch', 
    queries,
    data => {
      var hits = data.responses.map(r => replaceNulls(r.hits.hits[0]));
      var max = hits.reduce((z, h) => h._score > z ? h._score : z, 0);
      result.empty().append(searchResult({ took: 0, hits: { max_score: max, total: hits.length, hits: hits } }));
    }, 
    msg => null, 'POST', false, 'json');
}

//  <div class="header">
//  <label for="freeText">addresses</label>
//  <span class="example">one per line e.g.</span>
//  <span class="example multi-line">7 London Circuit, ACT 2601<br>18 London Circuit, ACT 2601</span>
//  <a id="clearFreeText" href="#">clear</a>
//</div>
//<textarea id="freeText" name="freeText"></textarea>
//</div>
//<button type="button">Search</button>}

function toStreet(h) {
  var s = h._source.street;
  var r = { street: s, text: filterJoin([ s.name, s.typeCode, s.suffixName ], ' ') };
  return r;
}

// filter array (preserving order) on unique values of f(x)
function unique(arr, f) {
  var set = new Set();
  function isNew(x) {
    var n = !set.has(x);
    if (n) set.add(x);
    return n;
  }
  return arr.filter(x => isNew(f(x)));
}

var hitsNearMe;
var unqStreet; 

function searchAddressesNearMe(coords, elem) {
  var loc = toQueryLoc(coords);
  var dist = getDist();
  debug('searchAddressesNearMe: loc =', loc, 'dist =', dist);
  if (!loc || !dist) {
    elem.empty().append([
      $('<span>').text('Please set a Location and Search distance then'),
      $('<a>').attr('href', '#').addClass('refresh').text('refresh').click(stopPropagation(() => searchAddressesNearMe(myCoords, elem)))
    ]);
  } else {
    runQuery(
      locationQuery(loc, dist, null, 1000),
      function(data) {
        hitsNearMe = data.hits.hits;
        unqStreet = unique(hitsNearMe.map(toStreet), s => s.text);
        
        var streetId = 'streetFilter';
        var streets = $('<select>').attr({id: streetId, name: streetId })
          .append($('<option>').attr('value', -1).text('Choose a nearby street'))
          .append(
            unqStreet.map((s, i) => $('<option>').attr('value', i).text(s.text))
          );
        
        // .empty() doesn't appear to work on <select>, so put it in a <span>
        var addrs = $('<span>');
        var result = $('<div>').attr('id', 'addressNearMeResult');
        
        elem.empty().append([
          $('<label>').attr('for', streetId).text('Filter by street'),
          streets, $('<br>'),
          $('<label>').attr('for', 'address').text('Choose address'),
          addrs, result ]);
        
        streets.selectmenu({ 
          change: function(ev, ui) {
            debug('searchAddressesNearMe: change: ev =', ev, 'ui =', ui, 'value =', ui.item.value);
            filterStreet(addrs, ui.item.value >= 0 ? unqStreet[ui.item.value].street : null, result);
          }
        }); 
        filterStreet(addrs, null, result);
      },
      function(errorThrown) {
        elem.empty();
      }
    );
  }
}


var streetHitsNearMe;

function filterStreet(addrs, street, result) {
  debug('filterStreet: street =', street);
  streetHitsNearMe = street ? hitsNearMe.filter(i => {
      var s = i._source.street;
      var b = s.name == street.name && s.typeCode == street.typeCode && s.suffixName == street.suffixName;
      return b;
    }) : hitsNearMe;
  addrs.empty();
  var sel = $('<select>').attr({id: 'address', name: 'address' })
    .append($('<option>').attr('value', -1).text('Choose an address'))
    .append(streetHitsNearMe.map((h, i) => $('<option>').attr('value', i).text(hitToAddress(h._source))));
  addrs.append(sel);
  sel.val(-1);
  sel.selectmenu({ 
    change: function(ev, ui) {
      debug('filterStreet: change: ev =', ev, 'ui =', ui, 'value =', ui.item.value);
      var hit = streetHitsNearMe[ui.item.value]._source;
      debug('filterStreet: hit =', hit);
      result.empty().append(searchResult({ took: 0, hits: { max_score: 1.0, total: 1, hits: [ { _score: 1.0, _source: hit } ] } }));
    }
  });
}

function initAutoCompleteAddress(elem, result) {
  elem.autocomplete({
    minLength: 4,
    source: suggestAddress,
    select: function(e, selected) {
      e.preventDefault();
      debug('address autocomplete: selected =', selected, 'value =', selected.item.value);
      elem.val(selected.item.value);
      result.empty().append(searchResult({ took: 0, hits: { max_score: 1.0, total: 1, hits: [ { _score: 1.0, _source: selected.item.payload } ] } }));
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
  runQuery(
    d61AddressQuery(toQueryLoc(myCoords), getDist(), req.term, 10),
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

function toQueryLoc(coords, dist) {
  return coords ? { lat: coords.latitude, lon: coords.longitude } : null;
}

function qDistance(loc, dist) {
  return { geo_distance : { distance : dist, location : loc } };
}

// filtered by location, but location doesn't affect the score
function d61AddressQuery(loc, dist, terms, size) {
  var qArr = [ { match: { d61Address: { query: flatSeparator(terms),  fuzziness: 2, prefix_length: 2 } } } ];
  if (loc && dist) {
    qArr.push(qDistance(loc, dist));
  }
  return { query: qArrToQuery(qArr), size: size };
}

function qArrToQuery(qArr) {
  return qArr.length == 1 ? qArr[0] : { bool : { must: qArr } };
}

function locationQuery(loc, dist, street, size) {
  var qArr = [ qDistance(loc, dist) ];
  if (street) {
    qArr.push({ match: { 'street.name': street.name } });
    qArr.push({ match: { 'street.typeCode': street.typeCode } });
    if (street.suffixName) qArr.push({ match: { 'street.suffixName': street.suffixName } });
  };
  return {
    query : { "function_score" : {
      query : qArrToQuery(qArr),
      exp: { location : {
        origin: loc,
        scale: dist
      }},
    }},
    size: size
  };
}

function search(inp, elem) {
  return function() {
    elem.empty();
    // addSpinner(elem);
    runQuery(
      d61AddressQuery(toQueryLoc(myCoords), getDist(), inp.val(), 10),
      function(data, textStatus, jqXHR) {
        elem.empty().append(searchResult(data));
      },
      function(msg) {
        elem.empty();
        // showError(elem, msg);
      }
    );
  };
}

function replaceNulls(h) {
  for (var p in h) {
    var obj = h[p];
    if (obj === 'D61_NULL' || obj === -1) h[p] = null;
    else if (typeof obj === 'object' && !Array.isArray(obj)) replaceNulls(obj);
  }
  return h;
}

function runQuery(query, success, error) {
  doAjax(
    esUrl + '_search', 
    JSON.stringify(query),
    data => {
      data.hits.hits.forEach(h => replaceNulls(h));
      success(data);      
    },
    error
  );
}

function doAjax(url, data, success, error, typ, contentType, dataType) {
  if (!typ) typ = data ? 'POST' : 'GET';
  if (!contentType) contentType = 'application/json; charset=utf-8';
  if (!dataType) dataType = 'json';
  try {
    debug('doAjax: url =', url, 'data =', data);
    $.ajax({
      type: typ,
      url: url,
      data: data,
      contentType: contentType,
      dataType: dataType,
      success: function(data, textStatus, jqXHR) {
        try {
          debug('doAjax success: data', data, 'textStatus', textStatus, 'jqXHR', jqXHR);
          success(data);
        } catch (e) {
          debug('doAjax success error: e = ', e);
          error(e.message);
        }
      },
      error: function(jqXHR, textStatus, errorThrown) {
        debug('doAjax ajax error: jqXHR =', jqXHR, 'textStatus =', textStatus, 'errorThrown =', errorThrown);
        error(errorThrown);
      }
    });
  } catch (e) {
    debug('doAjax error: e =', e);
    error(e.message);
  }
}

function filterJoin(arr, sep) {
  return arr.filter(x => x !== null && x !== '').join(sep)
}

function stopPropagation(f) {
  return function(ev) {
    ev.stopPropagation();
    f();
    return false;
  };
}

function siteColHandler(h) {
  return filterJoin([ h.addressSiteName, h.buildingName ], ' ');
}

function namePreNumSuf(name, n) {
  return filterJoin([name, filterJoin([n.prefix, n.number, n.suffix], '') ], ' ');
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

function locationColHandler(geoDetail) {
  return function(r) {
    var l = r.location
    return [ $('<span>').text(l.lat + ', ' + l.lon), $('<a>').attr('href', '#').addClass('showGeoDetail').text('…').click(stopPropagation(showGeoDetail(geoDetail, r.addressDetailPid))) ];
  };
}

function showGeoDetail(elem, addressDetailPid) {
  return function() {
    elem.empty();
    doAjax(dbUrl + 'addressGeocode/' + addressDetailPid, null,
      function(data) {
        elem.append(genTable(data, [
          new Col('Default', 'isDefault', d => d ? '*' : ''),
          new Col('Latitude', 'latitude'),
          new Col('Longitude', 'longitude'),
          new Col('Reliability', 'reliabilityCode'),
          new Col('Type', 'geocodeTypeDescription')
        ]));
      }, 
      function() {}
    );
  }
}

function hitToAddress(h) {
  return filterJoin([
    siteColHandler(h),
    filterJoin([ flatColHandler(h), levelColHandler(h), streetColHandler(h) ], ' '),
    filterJoin([ h.localityName, h.stateAbbreviation, h.postcode ], ' ')
  ], ', ');
}

function dataHits(data) {
  return data.hits.hits.map(h => {
    var obj = h._source;
    obj.score = h._score / data.hits.max_score;
    obj.record = obj; // for colHandler to access whole record
    return obj;
  });
}

function searchResult(data) {
  var geoDetail = $('<div>').addClass('geoDetail');
  return [
    $('<span>').attr('class', 'stats').text(data.hits.hits.length.toLocaleString() + ' of ' + data.hits.total.toLocaleString() + ' hits in ' + (data.took/1000.0).toFixed(3) + ' sec'),
    genTable(dataHits(data), [
                              new Col('Rank', 'score', scoreColHandler),
                              new Col('Site', 'record', siteColHandler),
                              new Col('Flat', 'record', flatColHandler),
                              new Col('Level', 'record', levelColHandler),
                              new Col('Street', 'record', streetColHandler),
                              new Col('Locality', 'localityName'),
                              new Col('Postcode', 'postcode'),
                              new Col('State', 'stateName'),
                              new Col('Location', 'record', locationColHandler(geoDetail))
                            ]),
    geoDetail
  ];
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


// Rest is copied over from old app, not used

//function initSearch() {
//  initBaseUrl();
//  $('#state').append(states.map(s => {
//    var opt = $('<option>').val(s.code);
//    opt.text(s.name);
//    return opt;
//  }));
//  $('#searchForm button').on('click', stopPropagation(search));
//  $('#clearFreeText').on('click', stopPropagation(clearFreeText));
//  $('#clearFields').on('click', stopPropagation(clearFields));
//  initSuggestAddress();
//  
//}
//
//function initBulk() {
//  initBaseUrl();
//  $('#searchForm button').on('click', stopPropagation(bulk));
//  $('#clearFreeText').on('click', stopPropagation(clearFreeText));
//}
//
//
//var states = [
//              { code: 'ACT', name: 'AUSTRALIAN CAPITAL TERRITORY' },
//              { code: 'NSW', name: 'NEW SOUTH WALES' },
//              { code: 'NT', name: 'NORTHERN TERRITORY' },
//              { code: 'OT', name: 'OTHER TERRITORIES' },
//              { code: 'QLD', name: 'QUEENSLAND' },
//              { code: 'SA', name: 'SOUTH AUSTRALIA' },
//              { code: 'TAS', name: 'TASMANIA' },
//              { code: 'VIC', name: 'VICTORIA' },
//              { code: 'WA', name: 'WESTERN AUSTRALIA' }
//            ];
//
//
//function clearFreeText() {
//  $('#freeText').val('');
//}
//
//var fields = [ 'site', 'level', 'flat', 'street', 'locality', 'postcode', 'state' ];
//
//function clearFields() {
//  fields.forEach(a => $('#' + a).val(a === 'state' ? 'ACT' : ''));
//}
//
///**
// * @returns object with a key named after each element in `fields` with trimmed, uppercased value taken from the DOM element with that id. 
// */
//function getFields() {
//  return fields.reduce(
//    (obj, f) => {
//      obj[f] = $('#' + f).val().trim().toUpperCase();
//      return obj;
//    },
//    ({})
//  );
//}
//
//
//function bulk() {
//  var r = $('#searchResult');
//  var tbl = genTable([], searchResultCols);
//  r.empty().append(tbl);
//  
//  function q(terms) {
//    runQuery(
//      d61AddressQuery(terms, 1),
//      function(data) {
//        data.hits.max_score = 1.0;
//        tbl.append(genRow(dataHits(data)[0], searchResultCols));
//      },
//      function() {}
//    );
//  }
//  
//  $('#freeText').val().split('\n').forEach(addr => q(addr));
//}
//
///**
//* Replace values used to represent `searchable nulls` with actual nulls.
//* <p>
//* Elastic search omits nulls from the index, consequently they cannot be searched for.
//* The indexing process substitutes 'D61_NULL' for null values to make them searchable and here we do the reverse.
//* 
//* @param h
//*/
//
//
//
//// add an .onEnter function we can use for input fields
//// (function($) {
//// $.fn.onEnter = function(func) {
//// this.bind('keypress', function(e) {
//// if (e.keyCode == 13) func.apply(this, [ e ]);
//// });
//// return this;
//// };
//// })(jQuery);
//
//function addSpinner(elem) {
//  elem.append('<div class="spinner"><img src="ajax-loader.gif" alt="spinner"></div>');
//}
//
//function showError(elem, error) {
//  elem.append($('<div>').attr('class', 'error').text(error));
//}

