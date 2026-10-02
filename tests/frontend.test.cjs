const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

function setup(fetch) {
  const elements = new Map();
  const element = selector => {
    if (!elements.has(selector)) elements.set(selector, {
      value: '', textContent: '', innerHTML: '', disabled: false,
      classList: { add() {}, remove() {}, toggle() {} },
      addEventListener() {}, setAttribute() {}, removeAttribute() {},
      append() {}, close() {}, showModal() {}, remove() {}
    });
    return elements.get(selector);
  };
  const context = vm.createContext({
    document: { querySelector: element, querySelectorAll: () => [], addEventListener() {}, createElement: () => element('toast') },
    localStorage: { getItem: () => null, setItem() {}, removeItem() {} },
    window: { scrollTo() {} }, history: {}, setTimeout: () => 0,
    requestAnimationFrame: fn => fn(), clearTimeout() {}, fetch, AbortSignal, URLSearchParams, Date, Map, console
  });
  vm.runInContext(fs.readFileSync(path.join(__dirname, '../src/main/resources/static/assets/app.js'), 'utf8'), context);
  vm.runInContext("state.token = 'test'; state.user = {role:'ADMIN',sub:'admin'}", context);
  return { context, element, run: code => vm.runInContext(code, context) };
}
const response = data => ({ ok: true, status: 200, json: async () => ({ code: 'OK', data }) });

test('admin loads server page and renders row actions', async () => {
  const app = setup(async () => response({ content: [{requestNo:'RR1',category:'FOOD',quantity:2,district:'象山区',status:'PENDING'}], totalElements:21 }));
  await app.run('refreshRequests()');
  assert.match(app.element('#activityList').innerHTML, /data-allocate="RR1"/);
  assert.equal(app.element('#nextPage').disabled, false);
  assert.equal(app.element('#prevPage').disabled, true);
  assert.equal(app.run('state.loading'), false);
});
test('failed pagination restores the last loaded page and unlocks refresh', async () => {
  const app = setup(async () => { throw new Error('offline'); });
  app.run('state.page = 2; state.loadedPage = 1; state.total = 50');
  await app.run('refreshRequests()');
  assert.equal(app.run('state.page'), 1);
  assert.equal(app.run('state.loading'), false);
});
test('citizen fetches server records without any local storage history', async () => {
  let url;
  const app = setup(async path => { url = path; return response({content:[{requestNo:'server-only',status:'PENDING'}],totalElements:1}); });
  app.run("state.user.role = 'CITIZEN'");
  await app.run('refreshRequests()');
  assert.match(url, /page=0&size=20/);
  assert.equal(app.run('state.requests[0].requestNo'), 'server-only');
});
test('late filter response cannot overwrite a newer query', async () => {
  const resolvers = [];
  const app = setup(() => new Promise(resolve => resolvers.push(resolve)));
  const older = app.run('refreshRequests()');
  app.element('#requestSearch').value = 'new';
  const newer = app.run('refreshRequests()');
  resolvers[1](response({content:[{requestNo:'new',status:'PENDING'}],totalElements:1}));
  await newer;
  resolvers[0](response({content:[{requestNo:'old',status:'PENDING'}],totalElements:1}));
  await older;
  assert.equal(app.run('state.requests[0].requestNo'),'new');
});
test('forbidden response keeps the user logged in', async () => {
  const app = setup(async () => ({ ok:false, status:403, json:async () => ({}) }));
  await assert.rejects(app.run("api('/api/test')"), /权限/);
  assert.equal(app.run('state.token'), 'test');
});
test('request rows escape untrusted content', () => {
  const app = setup();
  const html = app.run("requestRows([{requestNo:'<script>', category:'<img>', quantity:1,district:'<svg>',status:'PENDING'}])");
  assert.ok(!html.includes('<script>') && !html.includes('<img>') && !html.includes('<svg>'));
});
