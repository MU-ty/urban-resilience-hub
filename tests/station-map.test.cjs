const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
function setup() {
  const elements = new Map();
  function element(id) {
    if (!elements.has(id)) elements.set(id, {innerHTML:'',textContent:'',disabled:false,events:{},attrs:{},
      addEventListener(name, fn) {this.events[name]=fn;},
      setAttribute(name,value) {this.attrs[name]=value;},
      querySelector:element,querySelectorAll:()=>[],setPointerCapture(){},getBoundingClientRect:()=>({width:800})});
    return elements.get(id);
  }
  const context = vm.createContext({window:{},document:{querySelector:element}});
  vm.runInContext(fs.readFileSync(path.join(__dirname,'../src/main/resources/static/assets/station-map.js'),'utf8'),context);
  return {map:context.window.stationMap,element};
}
const station = {id:1,name:'象山共享站',district:'象山区',latitude:25.26,longitude:110.28,capacity:500,occupancy:120};
test('map reads coordinates and escapes station labels',()=>{
  const app=setup();app.map.render([{...station,name:'<img onerror=alert(1)>'}]);
  assert.match(app.element('#stationMap').innerHTML,/data-station="1"/);
  assert.ok(!app.element('#stationMap').innerHTML.includes('<img'));
  assert.match(app.element('#mapDetail').innerHTML,/380/);
  assert.match(app.element('#mapDetail').innerHTML,/25.2600/);
});
test('zoom has bounds and reset restores initial view after keyboard pan',()=>{
  const app=setup();app.map.render([station]);
  const initial=app.element('#mapScene').attrs.transform;
  for(let i=0;i<20;i++) app.element('#mapZoomIn').events.click();
  assert.equal(app.element('#mapZoomIn').disabled,true);
  app.element('#stationMap').events.keydown({key:'ArrowLeft',target:{closest:()=>null},preventDefault(){}});
  assert.notEqual(app.element('#mapScene').attrs.transform,initial);
  app.element('#mapReset').events.click();
  assert.equal(app.element('#mapScene').attrs.transform,initial);
  assert.equal(app.element('#mapZoomOut').disabled,true);
});
test('empty, missing-coordinate and failed states do not retain stale pins',()=>{
  const app=setup();app.map.render([station]);app.map.clear('查询失败');
  assert.ok(!app.element('#stationMap').innerHTML.includes('data-station='));
  assert.equal(app.element('#mapDetail').textContent,'查询失败');
  app.map.render([{...station,latitude:null}]);
  assert.ok(!app.element('#stationMap').innerHTML.includes('data-station='));
  assert.match(app.element('#mapDetail').innerHTML,/尚未提供坐标/);
  app.map.render([]);assert.match(app.element('#mapDetail').textContent,/暂无站点/);
});
