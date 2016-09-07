// TODO: Search swagger.json doesn't have return type of bulkSearch

Array.prototype.flatMap = function(f) {
  return this.map(f).flatten();
}
Array.prototype.flatten = function() {
  return Array.prototype.concat.apply([], this);
}

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

var searchUrl; // search service
var dbUrl; // GNAF (database) service
var contribUrl; // contrib (database) Service

function initBaseUrl() {
  var host = window.location.protocol === 'file:' ? 'http://localhost' : window.location.protocol + '//' + window.location.hostname;
  searchUrl = host + ':9040/';
  dbUrl = host + ':9000/gnaf/';
  contribUrl = host + ':9010/contrib/';
}

var searchDistance = [ 50, 100, 500, 1000, 2000, 5000, 10000, 50000, 100000 ];

function createLocation() {
  return $('<div>').addClass('location').append([ 
    $('<label>').text('Location'),
    $('<input>').attr({id: 'latitude', 'type': 'text'}), 
    $('<input>').attr({id: 'longitude', 'type': 'text'}), 
    $('<span>').attr({id: 'precision'}), 
    $('<a>').attr('href', '#').text('update').click(stopPropagation(updateLocation)),
    $('<br>'),
    $('<label>').text('Search distance'),
    $('<select>').attr('id', 'searchDistance')
      .append($('<option>').attr('value', -1).text("Don't use location"))
      .append(searchDistance.map(d => $('<option>').attr('value', d).text(d < 1000 ? d + 'm' : (d/1000) + 'km')))
  ]);
}

function updateLocation() {
  if ('geolocation' in navigator) {
    try {
      navigator.geolocation.getCurrentPosition(pos => {
        debug('updateLocation: coords =', pos.coords);
        $('#latitude').val(pos.coords.latitude.toString());
        $('#longitude').val(pos.coords.longitude.toString());
        $('#precision').text("±" + pos.coords.accuracy.toString() + 'm');
        var nextHigherDist = searchDistance.find(d => d >= pos.coords.accuracy);
        $('#searchDistance').val(nextHigherDist ? nextHigherDist : 5000);
      });
    } catch(e) {
      debug('updateLocation: error', e);
    }
  } else debug('updateLocation: geolocation not available');
}

function getLatLon() {
  var lat = Number($('#latitude').val());
  var lon = Number($('#longitude').val());
  var r = isNaN(lat + lon) || lat === 0 || lon === 0 ? null : { lat: lat, lon: lon };
  debug('getLatLon: r =', r);
  return r;
}

function getDist() {
  var v = $('#searchDistance').val();
  return v == -1 ? null : v;
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
      if (ui.newPanel.selector == sel) searchAddressesNearMe($('#addressNearMe'))
    } 
  });
  return tabs;
}


function createAddressLookup() {
  var id = 'addressInput';
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
        $('<span>').text('7 London Circuit ACT 2601'),
        $('<br>'),
        $('<span>').text('18 London Circuit ACT 2601')
      ]),
      $('<a>').attr('href', '#').text('clear').click(stopPropagation(() => textarea.val('')))
    ]),
    textarea,
    $('<br>'),
    $('<button>').attr('type', 'button').text('Search').click(stopPropagation(() => bulkSearch(textarea.val().split('\n').filter(a => a.trim() != ''), result))),
    result
  ]);
}

function bulkSearch(addresses, elem) {
  var q = d61AddressQuery(getLatLon(), getDist(), "", 1);
  delete q.addr;
  q.addresses = addresses;
  debug('bulkSearch: q =', q);
  
  showLoading(elem);
  var t0 = new Date().getTime();
  doAjax(
    searchUrl + 'bulkSearch', 
    JSON.stringify(q),
    data => { elem.empty().append(
      searchResult({ elapsedSecs: (new Date().getTime() - t0)/1000, totalHits: data.reduce((z, r) => z + r.totalHits, 0), hits: data.flatMap(r => r.hits) })
    )}, 
    msg => elem.empty(),
    'POST', false, 'json');
}

var streetToString = s => filterJoin([ s.name, s.typeCode, s.suffixName ], ' ');

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

function searchAddressesNearMe(elem) {
  var loc = getLatLon();
  var dist = getDist();
  debug('searchAddressesNearMe: loc =', loc, 'dist =', dist);
  if (!loc || !dist) {
    elem.empty().append([
      $('<span>').text('Please set a Location and Search distance then'),
      $('<a>').attr('href', '#').addClass('refresh').text('refresh').click(stopPropagation(() => searchAddressesNearMe(elem)))
    ]);
  } else {
    doAjax(
      searchUrl + 'search', 
      JSON.stringify(locationQuery(loc, dist, null, 1000)),
      function(data) {
        data.hits.forEach(h => h.gnaf = JSON.parse(h.json));
        hitsNearMe = data.hits;
        unqStreet = unique(hitsNearMe.map(h => ({ street: h.gnaf.street, text: streetToString(h.gnaf.street) })), s => s.text);
        
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
  streetHitsNearMe = street ? hitsNearMe.filter(h => {
      var s = h.gnaf.street;
      return s.name == street.name && s.typeCode == street.typeCode && s.suffixName == street.suffixName;
    }) : hitsNearMe;
    
  var loc = getLatLon();
  var cosLat = Math.cos(loc.lat * Math.PI/180);
  // distance from loc to l in meters
  function dist(l) {
    var dy = (l.lat - loc.lat) * 1e7/90;
    var dx = (l.lon - loc.lon) * 1e7/90 * cosLat;
    return Math.sqrt(dx * dx + dy * dy);
  }
  var sortBy = (a, b) => dist(a.gnaf.location) - dist(b.gnaf.location);
  streetHitsNearMe.sort(sortBy);
  
  debug('filterStreet: streetHitsNearMe =', streetHitsNearMe);
  addrs.empty();
  var sel = $('<select>').attr({id: 'address', name: 'address' })
    .append($('<option>').attr('value', -1).text('Choose an address'))
    .append(streetHitsNearMe.map((h, i) => $('<option>').attr('value', i).text(h.d61AddressNoAlias)));
  addrs.append(sel);
  sel.val(-1);
  sel.selectmenu({ 
    change: function(ev, ui) {
      debug('filterStreet: change: ev =', ev, 'ui =', ui, 'value =', ui.item.value);
      var hit = streetHitsNearMe[ui.item.value];
      debug('filterStreet: hit =', hit);
      result.empty().append(searchResult({ elapsedSecs: 0, totalHits: 1, hits: [hit]}));
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
      result.empty().append(searchResult({ elapsedSecs: 0, totalHits: 1, hits: [selected.item.payload]}));
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
  doAjax(
    searchUrl + 'search', 
    JSON.stringify(d61AddressQuery(getLatLon(), getDist(), req.term, 10)),
    function(data) {
      resp(data.hits.map(h => ({ label: h.d61AddressNoAlias, payload: h })));
    },
    function() {
      resp([]);
    }
  );
}

/**
 * Instead of "UNIT 2 12 BLAH STREET" people often use "2 / 12 BLAH STREET".
 * If the shingle filter is used to store bigrams "2 12 BLAH" will get a high score, but "2 / 12 BLAH" won't;
 * so we replace the "/" with " ".
 * 
 * Also replace commas with space in case anyone pastes CSV data.
 * 
 * @param addr
 * @returns
 */
function queryPreProcess(addr) {
  return addr.replace(/\/|,/g, ' ');
}

// filtered by location (doesn't affect query score)
function d61AddressQuery(loc, dist, terms, size) {
  var q = { addr: queryPreProcess(terms), numHits: size, fuzzy: { maxEdits: 2, minLength: 5, prefixLength: 2 } };
  if (loc && dist) {
    var dlat = 90 * dist/1e7 / 2; // 90 deg lat is about 1e7m (/2 for +/- dlat)
    var dlon = dlat * Math.cos(loc.lat * Math.PI/180);
    q["box"] = { minLat: loc.lat - dlat, minLon: loc.lon - dlon, maxLat: loc.lat + dlat, maxLon: loc.lon + dlon };
  }
  return q;
}

function locationQuery(loc, dist, street, size) {
  var q = d61AddressQuery(loc, dist, street ? filterJoin([ street.name, street.typeCode, street.suffixName ], ' ') : '', size);
  delete q.fuzzy;
  return q;
}

function showLoading(elem) {
  elem.empty().append($('<img>').attr({ alt: '', src: 'loading.gif', 'class': 'loading' }));
}

function search(inp, elem) {
  return function() {
    showLoading(elem);
    doAjax(
      searchUrl + 'search', 
      JSON.stringify(d61AddressQuery(getLatLon(), getDist(), inp.val(), 10)),
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

function doAjax(url, data, success, error, method, contentType, dataType) {
  if (!method) method = data ? 'POST' : 'GET';
  if (!contentType) contentType = 'application/json; charset=utf-8';
  if (!dataType) dataType = 'json';
  try {
    debug('doAjax: url =', url, 'data =', data);
    $.ajax({
      type: method,
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
    return [ $('<span>').text(l.lat + ', ' + l.lon), $('<a>').attr('href', '#').addClass('showGeoDetail').text('…').click(stopPropagation(showGeoDetail(geoDetail, r))) ];
  };
}

var existingGeocodeTypes = new Set();

function showGeoDetail(elem, r) { // addressDetailPid
  return function() {
    var addrType = $('<div>').addClass('addrType');
    var gnafGeocodes = $('<div>').addClass('gnafGeocodes');
    var contribGeocodes = $('<div>').addClass('contribGeocodes');
    elem.empty().append([addrType, gnafGeocodes, contribGeocodes]);
    showLoading(addrType);
    showLoading(gnafGeocodes);
    existingGeocodeTypes = new Set();
    
    doAjax(dbUrl + 'addressType/' + r.addressDetailPid, null,
      data => {
        addrType.empty().append([
          $('<label>').text('Address type'),
          $('<span>').text(data.addressType && data.addressType.addressType ? data.addressType.addressType : 'none')
        ]);
        if (data.addressType && data.addressType.addressSitePid) showContrib(contribGeocodes, data.addressType.addressSitePid);
      }, 
      err => addrType.empty()
    );
    
    doAjax(dbUrl + 'addressGeocode/' + r.addressDetailPid, null,
      data => { 
        gnafGeocodes.empty().append([
          $('<label>').text('G-NAF geocodes'),
          genTable(data, [
            new Col('Default', 'isDefault', d => d ? '*' : ''),
            new Col('Latitude', 'latitude'),
            new Col('Longitude', 'longitude'),
            new Col('Reliability', 'reliabilityCode'),
            new Col('Type', 'geocodeTypeDescription')
          ])
        ]);
        data.forEach(g => existingGeocodeTypes.add(g.geocodeTypeCode));
      }, 
      err => gnafGeocodes.empty()
    );
  }
}

// TODO: use Promises?

function showContrib(elem, addressSitePid) {

  function step2(geocodeTypes) {
    
    function refresh() {
      showLoading(elem);
      step2(geocodeTypes);
    }
    
    var typMap = geocodeTypes.reduce((z, x) => {
      z[x.code] = x.description;
      return z;
    }, {});
    debug('step2: typMap =', typMap);
    doAjax(contribUrl + addressSitePid, null,
      data => {
        data.forEach(d => d.record = d);
        var tbl = genTable(data, [
          new Col('Latitude', 'latitude'),
          new Col('Longitude', 'longitude'),
          new Col('Type', 'geocodeTypeCode', t => typMap[t]),
          new Col('Status', 'record', r => [
            $('<span>').text(r.contribStatus), 
            $('<a>').addClass('delete').attr('href', '#').text('delete').click(stopPropagation(() =>
              deleteContrib({id: r.id, version: r.version}, refresh)
            ))
          ], 'contribStatus')
        ]);
        data.forEach(g => existingGeocodeTypes.add(g.geocodeTypeCode));
        var mkInp = (id, val) => $('<td>').addClass(id).append($('<input>').attr({id: id, name: id, type: 'text'}).val(val));
        var latLon = getLatLon();
        tbl.append($('<tr>').addClass('add').append([
          mkInp('latitude', latLon ? latLon.lat : ''),
          mkInp('longitude', latLon ? latLon.lon : ''),
          $('<td>').addClass('geocodeTypeCode').append($('<select>').attr('id', 'geocodeTypeCode').append(geocodeTypes.filter(t => !existingGeocodeTypes.has(t.code)).map(t => $('<option>').attr('value', t.code).text(t.description)))),
          $('<td>').addClass('contribStatus').append(
            $('<a>').addClass('add').attr('href', '#').text('add').click(stopPropagation(() =>
              addContrib({ contribStatus: 'Submitted', addressSitePid: addressSitePid, geocodeTypeCode: $('#geocodeTypeCode').val(), longitude: Number($('#longitude').val()), latitude: Number($('#latitude').val()), dateCreated: 0, version: 0 }, refresh)
            ))
          )
        ]));
        elem.empty().append([
          $('<label>').text('Contributed geocodes'),
          tbl
        ]);
      }, 
      err => elem.empty()
    );
  };
  
  showLoading(elem);
  doAjax(dbUrl + 'geocodeType', null,
      data => step2(data.types),
      err => elem.empty()
  );
}

function addContrib(contribGeocode, refresh) {
  debug('addContrib: contribGeocode =', contribGeocode);
  doAjax(
    contribUrl,
    JSON.stringify(contribGeocode),
    data => {
      debug('addContrib: OK');
      refresh();
    },
    msg => {}
  );
}

function deleteContrib(contribGeocodeKey, refresh) {
  debug('addContrib: contribGeocodeKey =', contribGeocodeKey);
  doAjax(
    contribUrl,
    JSON.stringify(contribGeocodeKey),
    data => {
      debug('deleteContrib: OK');
      refresh();
    },
    msg => {},
    'DELETE'
  );
}

function dataHits(data) {
  return data.hits.map(h => {
    var obj = JSON.parse(h.json);
    obj.score = h.score;
    obj.record = obj; // for colHandler requiring access to multiple fields/whole record
    return obj;
  });
}

function searchResult(data) {
  var geoDetail = $('<div>').addClass('geoDetail');
  var locHandler = locationColHandler(geoDetail);
  return [
    $('<span>').attr('class', 'stats').text(data.hits.length.toLocaleString() + ' of ' + data.totalHits.toLocaleString() + ' hits in ' + data.elapsedSecs.toFixed(3) + ' sec'),
    genTable(dataHits(data), [
                              new Col('Rank', 'score', scoreColHandler),
                              new Col('Site', 'record', siteColHandler),
                              new Col('Flat', 'record', flatColHandler),
                              new Col('Level', 'record', levelColHandler),
                              new Col('Street', 'record', streetColHandler),
                              new Col('Locality', 'localityName'),
                              new Col('Postcode', 'postcode'),
                              new Col('State', 'stateName'),
                              new Col('Location', 'record', locHandler)
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
    tr.append($('<th>').addClass(col.tdClass).text(cols[i].label));
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
    tr.append($('<td>').addClass(col.tdClass).append(col.handler(x[col.field])));
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
