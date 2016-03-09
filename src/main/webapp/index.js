$(document).ready(init);

function init() {
  // initBaseUrl();
  $('#searchForm button').on('click', search);
}

var baseUrl = 'http://localhost:9200/gnaf/';

//function initBaseUrl() {
//baseUrl = window.location.protocol === 'file:'
//? 'localhost:9200/gnaf/' // use this when page served from a local file
//during dev
//: '/analytics/rest/v1.0'; // use this when page served from webapp
//}

function search() {
  var url = baseUrl + '_search';
  var elem = $('#searchResult');
  try {
    elem.empty();
    addSpinner(elem);
    var params = searchQuery($('#query').val(), $('#heuristics').is(':checked'));
    $.ajax({
      type: 'POST',
      url: url,
      data: JSON.stringify(params),
      dataType: 'json',
      success: function(data, textStatus, jqXHR) {
        try {
          debug('search success:', 'url', url, 'params', params, 'data', data, 'textStatus', textStatus, 'jqXHR', jqXHR);
          elem.empty();
          elem.append(searchResult(data));
        } catch (e) {
          debug('search success error: url = ', url, 'e = ', e);
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

// Address formats: although anything goes, Australian addresses often end with: Locality, State, PostCode
// Last two should be pretty easy to find

/** Find postCode as last 4 digit word with no subsequent digits.
  * returns { str: `s with postCode removed`, postCode: 1234 ], or if postCode not found { str: s, postCode: null }.
  */
function extractPostcode(s) {
  var re = /\b(\d{4})\b[^\d]*$/;
  var idx = s.search(re);
  return idx === -1 ? { str: s, postCode: null } : { str: s.slice(0, idx) + s.slice(idx + 4), postCode: s.slice(idx, idx + 4) };
}

/** Find last state.
  * returns { str: `s with state removed`, state: 'ACT' ], or if state not found { str: s, state: null }.
  */
function extractState(s) {
  var re = /\b(AUSTRALIAN\s+CAPITAL\s+TERRITORY|ACT|NEW\s+SOUTH\s+WALES|NSW|NORTHERN\s+TERRITORY|NT|OTHER\s+TERRITORIES|OT|QUEENSLAND|QLD|SOUTH\s+AUSTRALIA|SA|TASMANIA|TAS|VICTORIA|VIC|WESTERN\s+AUSTRALIA|WA)\b/ig;
  var arr = null;
  var x;
  while (null != (x = re.exec(s))) arr = x;
  return arr === null ? { str: s, state: null } : { str: s.slice(0, arr.index) + s.slice(arr.index + arr[0].length), state: arr[0] }
}

function searchQuery(query, heuristics) {
//  debug('searchQuery: query = ', query, 'heuristics = ', heuristics);
//  if (heuristics) {
//    var reRange = /(\d+)\s*-\s*(\d+)/;
//    var arr = reLastNumber.exec(query);
//    if (arr != null && arr.length > 0 && arr[1].length == 4) {
//      var postcode = arr[1];
//    }
//  }
  
  return {
    query: { match: { "_all": { query: query, fuzziness: 1, prefix_length: 2 } } },
    size: 10
  };
}

/*
 * {"took":49,"timed_out":false,"_shards":{"total":5,"successful":5,"failed":0},"hits":{"total":1214,"max_score":6.0664215,"hits":[{"_index":"gnaf","_type":"gnaf","_id":"GAACT717685921","_score":6.0664215,"_source":{
 *   "addressDetailPid":"GAACT717685921","addressSiteName":null,"buildingName":null,
 *   "flatTypeCode":"UNIT","flat":{"prefix":null,"number":16,"suffix":null},
 *   "levelName":null,"level":{"prefix":null,"number":null,"suffix":null},
 *   "numberFirst":{"prefix":null,"number":61,"suffix":null},
 *   "numberLast":{"prefix":null,"number":null,"suffix":null},
 *   "street":{"name":"CURRONG","typeCode":"STREET","suffixCode":"N"},
 *   "localityName":"BRADDON","stateAbbreviation":"ACT","stateName":"AUSTRALIAN CAPITAL TERRITORY","postcode":"2612",
 *   "location":{"lat":-35.27700812,"lon":149.13506014},
 *   "streetVariant":[{"name":"CURRONG","typeCode":"STREET","suffixCode":null},{"name":"CURRONG STREET","typeCode":"NORTH","suffixCode":null}],
 *   "localityVariant":[{"localityName":"CANBERRA CENTRAL"},{"localityName":"CITY"}]}}
 */
function searchResult(data) {
  return data.hits.hits.map(hit => {
    var h = hit._source;
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

//debug and error logging

function debug() {
  console.log(arguments)
}

//add an .onEnter function we can use for input fields
//(function($) {
//$.fn.onEnter = function(func) {
//this.bind('keypress', function(e) {
//if (e.keyCode == 13) func.apply(this, [ e ]);
//});
//return this;
//};
//})(jQuery);

function addSpinner(elem) {
  elem.append('<div class="spinner"><img src="ajax-loader.gif" alt="spinner"></div>');
}

function showError(elem, error) {
  elem.append($('<div>').attr('class', 'error').text(error));
}

//6 hits in 0.0250 sec
//function stats(data) {
//  return $('<span>').attr('class', 'stats').text(data.totalHits + ' hits in ' + data.elapsedSecs.toFixed(3) + ' sec');
//}

