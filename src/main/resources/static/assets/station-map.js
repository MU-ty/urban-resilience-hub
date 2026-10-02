/* Local schematic, deliberately not a navigation map. Coordinates come from the API. */
(() => {
  const map = document.querySelector('#stationMap');
  const detail = document.querySelector('#mapDetail');
  let stations = [], selected = null, zoom = 1, offset = {x:0,y:0}, drag = null;
  const safe = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const point = s => ({x: (Number(s.longitude) - 110.25) / .10 * 800, y: (25.30 - Number(s.latitude)) / .08 * 400});
  const located = s => s.latitude != null && s.longitude != null && Number.isFinite(Number(s.latitude)) && Number.isFinite(Number(s.longitude)) && Math.abs(Number(s.latitude)) <= 90 && Math.abs(Number(s.longitude)) <= 180;
  function transform() {
    const group = map.querySelector('#mapScene');
    if (group) group.setAttribute('transform', `translate(${400 + offset.x} ${200 + offset.y}) scale(${zoom}) translate(-400 -200)`);
    document.querySelector('#mapZoomOut').disabled = zoom <= 1;
    document.querySelector('#mapZoomIn').disabled = zoom >= 3;
  }
  function select(id) {
    selected = stations.find(s => String(s.id) === String(id));
    if (!selected) return;
    const s = selected, remaining = Math.max(0, s.capacity - s.occupancy);
    const percent = s.capacity > 0 ? Math.min(100, Math.max(0, Math.round(s.occupancy / s.capacity * 100))) : 0;
    detail.innerHTML = `<span class="map-eyebrow">${safe(s.district)} · 站点详情</span><h2>${safe(s.name)}</h2><p class="map-remaining"><b>${remaining}</b> 个剩余名额</p><label for="stationCapacity">接纳情况 <span>${safe(s.occupancy)} / ${safe(s.capacity)} 人</span></label><progress id="stationCapacity" max="100" value="${percent}">${percent}%</progress><p class="map-caption">${located(s) ? `演示坐标 ${Number(s.latitude).toFixed(4)}° N, ${Number(s.longitude).toFixed(4)}° E` : '该站点尚未提供坐标'}</p><button class="primary-btn" data-action="new-request">在此辖区提交求助</button>`;
    map.querySelectorAll('[data-station]').forEach(el => {
      const active = el.dataset.station === String(s.id);
      el.classList.toggle('selected', active);
      el.setAttribute('aria-pressed', String(active));
    });
  }
  function draw() {
    const pins = stations.filter(located).map(s => {
      const p = point(s);
      return `<g class="map-pin" data-station="${safe(s.id)}" transform="translate(${p.x} ${p.y})" tabindex="0" role="button" aria-label="查看${safe(s.name)}" aria-pressed="false"><circle r="19" class="pin-halo"/><circle r="8" class="pin-dot"/><text y="-29" text-anchor="middle">${safe(s.name)}</text></g>`;
    }).join('');
    map.innerHTML = `<svg viewBox="0 0 800 400" aria-label="桂林站点分布示意图"><g id="mapScene"><rect x="-2000" y="-2000" width="4800" height="4400" fill="#edf0e8"/><g fill="#e1e8d9"><path d="M40 35L180 45 200 120 80 150Z"/><path d="M570 35L740 60 710 145 590 130Z"/><path d="M80 300L220 275 280 380 120 390Z"/><path d="M580 290L730 280 760 390 610 380Z"/></g><g fill="none" stroke="#fff" stroke-width="12"><path d="M-50 150L850 145M-50 290L850 305M150 -50L240 450M610 -50L540 450"/></g><path d="M410 -100C320 20 460 80 375 175S410 320 370 500" fill="none" stroke="#b6d4dd" stroke-width="35"/><path d="M335 150L427 150M355 298L419 300" stroke="#fbfcf8" stroke-width="12"/><g fill="#839183" font-size="18"><text x="130" y="245">象山区</text><text x="585" y="230">七星区</text><text x="432" y="82" fill="#698e9b" font-size="14">漓江（示意）</text></g>${pins}</g><text x="765" y="30" text-anchor="middle" fill="#64746e" font-size="12">N ↑</text></svg>`;
    transform();
  }
  function reset() {
    zoom = 1; offset = {x:0,y:0};
    const points = stations.filter(located).map(point);
    if (points.length) offset = {x:400 - points.reduce((n,p) => n+p.x,0)/points.length, y:200 - points.reduce((n,p) => n+p.y,0)/points.length};
    transform();
  }
  window.stationMap = {
    render(values) {
      stations = values; selected = null; draw(); reset();
      if (stations.length) select(stations[0].id);
      else detail.textContent = '当前辖区暂无站点，可切换辖区查询。';
    },
    clear(message) { stations = []; selected = null; draw(); reset(); detail.textContent = message; }
  };
  document.querySelector('#mapZoomIn').addEventListener('click', () => { zoom = Math.min(3, zoom + .25); transform(); });
  document.querySelector('#mapZoomOut').addEventListener('click', () => { zoom = Math.max(1, zoom - .25); transform(); });
  document.querySelector('#mapReset').addEventListener('click', reset);
  map.addEventListener('click', e => { const pin = e.target.closest('[data-station]'); if (pin) select(pin.dataset.station); });
  map.addEventListener('keydown', e => {
    const pin = e.target.closest('[data-station]');
    if (pin && ['Enter',' '].includes(e.key)) { e.preventDefault(); select(pin.dataset.station); return; }
    const delta = {ArrowLeft:[30,0],ArrowRight:[-30,0],ArrowUp:[0,30],ArrowDown:[0,-30]}[e.key];
    if (delta) { e.preventDefault(); offset.x += delta[0]; offset.y += delta[1]; transform(); }
  });
  map.addEventListener('pointerdown', e => {
    if (e.button !== 0 || e.target.closest('[data-station]')) return;
    drag = {id:e.pointerId,x:e.clientX,y:e.clientY}; map.setPointerCapture(e.pointerId);
  });
  map.addEventListener('pointermove', e => {
    if (!drag || drag.id !== e.pointerId) return;
    const scale = 800 / map.getBoundingClientRect().width;
    offset.x += (e.clientX-drag.x)*scale; offset.y += (e.clientY-drag.y)*scale;
    drag.x=e.clientX; drag.y=e.clientY; transform();
  });
  for (const event of ['pointerup','pointercancel','lostpointercapture']) map.addEventListener(event, () => { drag=null; });
  window.stationMap.clear('请选择辖区查看站点。');
})();
