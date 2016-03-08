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
    var params = searchQuery($('#query').val());
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

function searchQuery(s) {
  debug('searchQuery: s = ', s)
  return {
    query: { match: { "_all": { query: s, fuzziness: 1, prefix_length: 2 } } },
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

