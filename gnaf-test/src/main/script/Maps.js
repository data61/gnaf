'use strict';

var identity = x => x;

function mapToObj(m, f = identity) {
  var a = {};
  for (var [k, v] of m) a[k.toString()] = f(v);
  return a;    
}

var ctorMap = () => new Map();
var ctorArr = () => [];



/** Map where values are a container e.g. an array or another map */
class MapCont {
  
  /**
   * @param ctor constructor for a new container value in the map
   */
  constructor(ctor) {
    this.ctor = ctor;
    this.m = ctorMap();
  }
  
  /**
   * Get container for k, constructing and adding it if it doesn't exist.
   * @param k 
   */
  get(k) {
    var a = this.m.get(k);
    if (!a) {
      a = this.ctor();
      this.m.set(k, a);
    }
    return a;
  }
  
  /** convert to object (e.g. for JSON serialization) */
  object(f = identity) { return mapToObj(this.m, f); }
}



/** Map of maps: k1 -> k2 -> v */
class MapMap extends MapCont {
  
  constructor(ctor = ctorMap) {
    super(ctor);
  }
  
  get2(k1, k2) {
    return this.get(k1).get(k2);
  }
  
  set2(k1, k2, v) {
    this.get(k1).set(k2, v);
  }
  
  object(f = identity) { return super.object(v => mapToObj(v, f)); }
}



/** Map of histograms, where the histograms are maps: k2 -> count */
class MapHist extends MapMap {
  inc(k1, k2) {
    var m2 = this.get(k1);
    var n = m2.get(k2);
    m2.set(k2, n ? n + 1 : 1);
  }
}



/** Map of arrays */
class MapArr extends MapCont {
  
  constructor() {
    super(ctorArr);
  }
  
  append(k1, v) {
    this.get(k1).push(v);
  }
}

var ctorMapArr = () => new MapArr();



class MapMapCont extends MapMap {
  
  constructor(ctor) { // must provide object() 
    super(ctor);
  }
  
  object(f = identity) { return mapToObj(this.m, v => v.object(f)); }
}



module.exports = {
  identity: identity,
  mapToObj: mapToObj,
  ctorMapArr: ctorMapArr,
  MapCont: MapCont,
  MapMap: MapMap,
  MapHist: MapHist,
  MapArr: MapArr,
  MapMapCont: MapMapCont
}

// examples
//var m = new MapMap();
//m.set2("sally", 1, "fred")
//m.set2("sally", 1, "sally")
//m.set2("sally", 2, "fred")
//console.log('MapMap.object:', m.object());
//
//var h = new MapHist();
//h.inc("fred", 1);
//h.inc("sally", 2);
//h.inc("sally", 2);
//console.log('MapHist.object:', h.object());
//
//var a = new MapArr();
//a.append("sally", "fred");
//a.append("sally", "sue");
//console.log('MapArr.object:', a.object());
//
//var c = new MapMapCont(ctorMapArr);
//c.get("sally").append("george", "fred");
//c.get("sally").append("george", "sue");
//console.log('MapMapCont.object:', c.object());


