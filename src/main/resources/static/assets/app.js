let filterTimer;
let submissionAttempt;
const state = {
  token: localStorage.getItem('resilience.token'),
  user: null,
  district: '浦东新区',
  requests: [], page: 0, total: 0, loading: false, stationVersion: 0, refreshVersion: 0, loadedPage: 0
};

if ('scrollRestoration' in history) history.scrollRestoration = 'manual';

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const escapeHtml = value => String(value ?? '').replace(/[&<>'"]/g, char => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
})[char]);

function parseToken(token) {
  try {
    const payload = JSON.parse(decodeURIComponent(escape(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))));
    return payload.exp * 1000 > Date.now() ? payload : null;
  } catch { return null; }
}

async function api(path, options = {}) {
  const requestToken = state.token;
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  let response;
  try { response = await fetch(path, { ...options, headers, signal: AbortSignal.timeout(15000) }); }
  catch { throw new Error('连接失败或请求超时，请稍后重试'); }
  const body = await response.json().catch(() => ({}));
  if (response.status === 401) {
    if (path !== '/api/auth/login' && state.token === requestToken) logout(false);
    throw new Error(body.message || '登录状态已失效');
  }
  if (response.status === 403) throw new Error('当前账号没有此操作权限');
  if (!response.ok || (body.code && body.code !== 'OK')) throw new Error(body.message || '请求失败');
  return body.data;
}

function toast(title, detail = '', error = false) {
  const item = document.createElement('div');
  item.className = `toast${error ? ' error' : ''}`;
  item.innerHTML = `<i></i><div><b>${escapeHtml(title)}</b><span>${escapeHtml(detail)}</span></div>`;
  $('#toasts').append(item);
  setTimeout(() => item.remove(), 3800);
}

function showApp() {
  state.user = parseToken(state.token);
  if (!state.user) return logout(false);
  $('#loginView').classList.add('hidden');
  $('#appView').classList.remove('hidden');
  window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
  requestAnimationFrame(() => window.scrollTo(0, 0));
  setTimeout(() => window.scrollTo(0, 0), 80);
  const isAdmin = state.user.role === 'ADMIN';
  $$('.admin-only').forEach(el => el.classList.toggle('hidden', !isAdmin));
  const name = isAdmin ? '调度工作人员' : '居民用户';
  $('#displayName').textContent = name;
  $('#dashboardView .page-heading p').textContent = isAdmin ? '查看最新求助，核对需求后进行物资分配。' : '查看已提交的求助，刷新处理状态。';
  switchView('dashboard');
  state.page = 0;
  state.total = 0;
  $('#requestSearch').value = ''; $('#statusFilter').value = '';
  $('#recordsTitle').textContent = isAdmin ? '求助记录' : '我的记录';
  $('[data-view="requests"].nav-item').textContent = isAdmin ? '求助记录' : '我的记录';
  $('#recordsDescription').textContent = isAdmin ? '查看并处理平台收到的求助。筛选查询全部记录。' : '当前账号提交的全部求助，可跨设备查询。';
  $('#summaryScope').textContent = isAdmin ? '当前页待处理数量' : '当前页待处理数量';
  $('#recordScope').textContent = isAdmin ? '记录来自服务端；可进入全部记录翻页处理。' : '记录保存在服务器，可在其他设备登录查询。';
  state.requests = [];
  renderRequests();
  loadStations(state.district);
  refreshRequests();
}

function logout(notify = true) {
  $$('dialog[open]').forEach(dialog => dialog.close());
  state.token = null; state.user = null; state.requests = []; state.stationVersion++; state.refreshVersion++; state.loading = false;
  localStorage.removeItem('resilience.token');
  $('#appView').classList.add('hidden');
  $('#loginView').classList.remove('hidden');
  window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
  if (notify) toast('已退出登录');
}

async function login(event) {
  event.preventDefault();
  const button = event.submitter;
  button.disabled = true; button.querySelector('span').textContent = '正在验证身份…';
  $('#loginError').textContent = '';
  try {
    const data = await api('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username: $('#username').value.trim(), password: $('#password').value })
    });
    state.token = data.accessToken;
    localStorage.setItem('resilience.token', state.token);
    showApp();
    toast('登录成功');
  } catch (error) { $('#loginError').textContent = error.message; }
  finally { button.disabled = false; button.querySelector('span').textContent = '登录'; }
}

async function loadStations(district) {
  state.district = district;
  const version = ++state.stationVersion;
  $('#stationGrid').innerHTML = '<div class="empty-state" role="status">正在查询站点…</div>';
  $('#stationCount').textContent = '—';
  try {
    const stations = await api(`/api/shelters?district=${encodeURIComponent(district)}`);
    if (version !== state.stationVersion) return;
    $('#stationCount').textContent = stations.length;
    renderStations(stations);
    $('#lastRefresh').textContent = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  } catch (error) {
    if (version !== state.stationVersion) return;
    $('#lastRefresh').textContent = '查询失败';
    $('#stationGrid').innerHTML = `<div class="empty-state"><b>站点查询失败</b>${escapeHtml(error.message)}<br><button class="text-btn" data-action="retry-stations">重新查询</button></div>`;
  }
}

function renderStations(stations) {
  $('#stationGrid').innerHTML = stations.length ? `<table><thead><tr><th>站点名称</th><th>所在辖区</th><th>已接纳人数</th><th>总容量</th><th>剩余名额</th></tr></thead><tbody>${stations.map(station => `<tr><td>${escapeHtml(station.name)}</td><td>${escapeHtml(station.district)}</td><td>${escapeHtml(station.occupancy)} 人</td><td>${escapeHtml(station.capacity)} 人</td><td>${Math.max(0, station.capacity - station.occupancy)} 人</td></tr>`).join('')}</tbody></table>` : '<div class="empty-state"><b>暂无站点</b>可切换辖区重新查询。</div>';
}
function categoryName(value) { return ({ WATER: '饮用水', FOOD: '食品', MEDICINE: '医疗物资', POWER: '应急电力', HYGIENE: '卫生用品' })[value] || value; }
function statusName(value) { return ({ PENDING: '等待调度', ALLOCATED: '已完成分配', PARTIALLY_ALLOCATED: '部分分配' })[value] || value; }
function requestRows(rows) {
  if (!rows.length) return '<div class="empty-state"><b>暂无求助记录</b>新建求助后，记录将显示在这里。</div>';
  return `<table><thead><tr><th>所需物资 / 求助编号</th><th>数量</th><th>所在辖区</th><th>处理状态</th><th>操作</th></tr></thead><tbody>${rows.map(r => `<tr><td>${escapeHtml(categoryName(r.category))}<small class="request-id">${escapeHtml(r.requestNo)}</small></td><td>${escapeHtml(r.quantity)} 份</td><td>${escapeHtml(r.district)}</td><td><span class="status ${r.status === 'PENDING' ? 'pending' : 'allocated'}">${escapeHtml(statusName(r.status))}</span></td><td><button class="text-btn" data-detail="${escapeHtml(r.requestNo)}">详情</button>${state.user?.role === 'ADMIN' && r.status === 'PENDING' ? ` <button class="text-btn" data-allocate="${escapeHtml(r.requestNo)}">分配</button>` : ''}</td></tr>`).join('')}</tbody></table>`;
}
function renderRequests() {
  $('#requestCount').textContent = state.requests.filter(r => r.status === 'PENDING').length;
  $('#activityList').innerHTML = requestRows(state.requests.slice(0, 6));
  $('#requestTable').innerHTML = state.requests.length ? requestRows(state.requests) : '<div class="empty-state"><b>没有匹配的求助</b>可清空筛选，或新建求助。</div>';
  const pages = Math.max(1, Math.ceil(state.total / 20));
  $('#recordTotal').textContent = '共 ' + state.total + ' 条';
  $('#pageNumber').textContent = (state.page + 1) + ' / ' + pages;
  $('#prevPage').disabled = state.page === 0 || state.loading;
  $('#nextPage').disabled = state.page + 1 >= pages || state.loading;
  for (const selector of ['#activityList', '#requestTable']) $(selector).setAttribute('aria-busy', String(state.loading));
}

async function createRequest(event) {
  event.preventDefault();
  if (event.submitter.disabled) return;
  const form = new FormData(event.currentTarget);
  const body = { district: form.get('district'), category: form.get('category'), quantity: Number(form.get('quantity')), priority: form.get('priority') };
  const button = event.submitter;
  const fingerprint = JSON.stringify(body);
  if (!submissionAttempt || submissionAttempt.fingerprint !== fingerprint) submissionAttempt = {fingerprint, key:crypto.randomUUID()};
  button.disabled = true;
  try {
    const created = await api('/api/relief-requests', {
      method: 'POST',
      headers: { 'Idempotency-Key': submissionAttempt.key },
      body: JSON.stringify(body)
    });
    state.requests.unshift({ ...created, createdAtLabel: '刚刚' });
    if (state.user?.role === 'ADMIN') state.total++;
    submissionAttempt = null;
    $('#requestForm').reset();
    $('#requestSearch').value = ''; $('#statusFilter').value = ''; state.page = 0;
    refreshRequests();
    $('#requestModal').close();
    toast('求助已提交', `编号 ${created.requestNo}`);
  } catch (error) { toast('提交失败', error.message, true); }
  finally { button.disabled = false; }
}

async function allocate(event) {
  event.preventDefault();
  if (event.submitter.disabled) return;
  const requestNo = new FormData(event.currentTarget).get('requestNo').trim();
  const button = event.submitter; button.disabled = true;
  try {
    const result = await api(`/api/relief-requests/${encodeURIComponent(requestNo)}/allocate`, { method: 'POST' });
    const local = state.requests.find(r => r.requestNo === requestNo);
    if (local) local.status = result.unallocatedQuantity === 0 ? 'ALLOCATED' : 'PARTIALLY_ALLOCATED';
    renderRequests(); $('#allocateModal').close();
    toast('分配完成', `已分配 ${result.allocatedQuantity} 份，未分配 ${result.unallocatedQuantity} 份`);
    refreshRequests();
  } catch (error) { toast('调度未完成', error.message, true); }
  finally { button.disabled = false; }
}

function switchView(name) {
  $$('.view').forEach(v => v.classList.remove('active'));
  $(`#${name}View`)?.classList.add('active');
  $$('.nav-item').forEach(item => item.classList.toggle('active', item.dataset.view === name));
  $('#pageTitle').textContent = ({ dashboard: '求助工作台', requests: state.user?.role === 'ADMIN' ? '求助记录' : '我的记录', stations: '共享站查询' })[name] || '求助工作台';
  $$('.nav-item[data-view]').forEach(item => { if (item.dataset.view === name) item.setAttribute('aria-current', 'page'); else item.removeAttribute('aria-current'); });
  $('.sidebar').classList.remove('open');
  window.scrollTo({ top: 0, left: 0, behavior: 'smooth' });
}

document.addEventListener('click', event => {
  const close = event.target.closest('[data-close]');
  if (close) document.getElementById(close.dataset.close).close();
  const view = event.target.closest('[data-view]');
  if (view) switchView(view.dataset.view);
  const action = event.target.closest('[data-action]')?.dataset.action;
  if (action === 'new-request') { $('#district').value = state.district; $('#requestModal').showModal(); }
  if (action === 'refresh-requests') refreshRequests(true);
  if (action === 'retry-stations') loadStations(state.district);
  const detail = event.target.closest('[data-detail]');
  if (detail) showDetail(detail.dataset.detail);
  const allocation = event.target.closest('[data-allocate]');
  if (allocation) { $('#requestNo').value = allocation.dataset.allocate; $('#allocateModal').showModal(); }
  if (action === 'allocate') $('#allocateModal').showModal();
  const quick = event.target.closest('[data-user]');
  if (quick) { $('#username').value = quick.dataset.user; $('#password').value = quick.dataset.password; }
});

$('#loginForm').addEventListener('submit', login);
$('#requestForm').addEventListener('submit', createRequest);
$('#allocateForm').addEventListener('submit', allocate);
$('#logoutBtn').addEventListener('click', () => logout());
$('#requestSearch').addEventListener('input', () => {
  clearTimeout(filterTimer);
  state.page = 0;
  filterTimer = setTimeout(() => refreshRequests(), 350);
});
$('#statusFilter').addEventListener('change', () => { clearTimeout(filterTimer); state.page = 0; refreshRequests(); });
$('#clearFilters').addEventListener('click', () => { clearTimeout(filterTimer); $('#requestSearch').value = ''; $('#statusFilter').value = ''; state.page = 0; refreshRequests(); });
for (const [id, delta] of [['prevPage', -1], ['nextPage', 1]]) $('#' + id).addEventListener('click', () => { state.page += delta; refreshRequests(); });
$('#refreshBtn').addEventListener('click', () => loadStations(state.district));
$$('[data-district]').forEach(button => button.addEventListener('click', () => {
  $$('[data-district]').forEach(b => b.classList.remove('active')); button.classList.add('active'); loadStations(button.dataset.district);
}));


async function refreshRequests(notify = false) {
  if (!state.user) return;
  const token = state.token;
  const version = ++state.refreshVersion;
  const params = new URLSearchParams({page:state.page, size:20});
  const keyword = $('#requestSearch').value.trim();
  if (keyword) params.set('keyword', keyword);
  if ($('#statusFilter').value) params.set('status', $('#statusFilter').value);
  state.loading = true;
  renderRequests();
  $$('[data-action="refresh-requests"]').forEach(b => { b.disabled = true; b.textContent = '正在刷新…'; });
  try {
    const page = await api('/api/relief-requests?' + params);
    if (state.token !== token || version !== state.refreshVersion) return;
    state.requests = page.content; state.total = page.totalElements; state.loadedPage = state.page;
    if (notify) toast('记录状态已更新');
  } catch (error) {
    if (state.token === token && version === state.refreshVersion) {
      state.page = state.loadedPage;
      toast('记录刷新失败', error.message, true);
      for (const selector of ['#activityList', '#requestTable']) $(selector).innerHTML = '<div class="empty-state"><b>记录加载失败</b>' + escapeHtml(error.message) + '<br><button class="text-btn" data-action="refresh-requests">重新加载</button></div>';
      return;
    }
  } finally {
    if (version === state.refreshVersion) {
      state.loading = false;
      $$('[data-action="refresh-requests"]').forEach(b => { b.disabled = false; b.textContent = '刷新状态'; });
      for (const selector of ['#activityList', '#requestTable']) $(selector).setAttribute('aria-busy', 'false');
    }
  }
  if (state.token === token && version === state.refreshVersion) renderRequests();
}
async function showDetail(no) {
  $('#detailBody').innerHTML = '<p role="status">正在查询…</p>';
  $('#detailModal').showModal();
  try {
    const r = await api('/api/relief-requests/' + encodeURIComponent(no));
    const date = new Date(r.createdAt);
    const fields = [['求助编号', r.requestNo], ['所需物资', categoryName(r.category)], ['需求数量', r.quantity + ' 份'], ['所在辖区', r.district], ['紧急程度', ({NORMAL:'普通',HIGH:'紧急',CRITICAL:'特急'})[r.priority] || r.priority], ['处理状态', statusName(r.status)], ['提交时间', Number.isNaN(date.getTime()) ? '—' : date.toLocaleString('zh-CN', {hour12:false})]];
    $('#detailBody').innerHTML = '<dl class="detail-list">' + fields.map(([label,value]) => '<div><dt>' + label + '</dt><dd>' + escapeHtml(value) + '</dd></div>').join('') + '</dl>';
    const local = state.requests.find(item => item.requestNo === no);
    if (local) { Object.assign(local, r); renderRequests(); }
  } catch(error) { $('#detailBody').innerHTML = '<p role="alert">' + escapeHtml(error.message) + '</p>'; }
}

if (state.token && parseToken(state.token)) showApp(); else logout(false);
