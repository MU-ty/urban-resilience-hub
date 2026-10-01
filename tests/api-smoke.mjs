// Uses the running local demo stack and creates TWO small test requests.
// One citizen request reserves one unit of WATER. Existing data is never deleted.
import assert from 'node:assert/strict';
const base = process.env.BASE_URL || 'http://localhost:18080';
async function call(path, token, options = {}) {
  const response = await fetch(base + path, { ...options, headers: {'Content-Type':'application/json', ...(token ? {Authorization:'Bearer '+token} : {}), ...options.headers} });
  return {status:response.status, body:await response.json()};
}
async function login(username,password) {
  const r = await call('/api/auth/login',null,{method:'POST',body:JSON.stringify({username,password})});
  assert.equal(r.status,200); return r.body.data.accessToken;
}
const citizen = await login('citizen',process.env.CITIZEN_PASSWORD || 'User@123');
const admin = await login('admin',process.env.ADMIN_PASSWORD || 'Admin@123');
assert.equal((await call('/api/relief-requests')).status,401);
const body = {district:'浦东新区',category:'WATER',quantity:1,priority:'NORMAL'};
const key = 'smoke-'+crypto.randomUUID();
const create = (token,k,data=body) => call('/api/relief-requests',token,{method:'POST',headers:{'Idempotency-Key':k},body:JSON.stringify(data)});
const first = await create(citizen,key);
assert.equal(first.status,200); assert.ok(first.body.data.createdAt);
const no = first.body.data.requestNo;
assert.equal((await create(citizen,key)).body.data.requestNo,no);
assert.equal((await create(citizen,key,{...body,quantity:2})).status,409);
const other = await create(admin,key);
assert.equal(other.status,200);
assert.equal((await call('/api/relief-requests/'+other.body.data.requestNo,citizen)).status,404);
assert.equal((await call('/api/relief-requests/'+no+'/allocate',citizen,{method:'POST'})).status,403);
assert.equal((await call('/api/relief-requests?page=-1',citizen)).status,400);
assert.equal((await call('/api/relief-requests?status=INVALID',citizen)).status,400);
const page = await call('/api/relief-requests?keyword='+encodeURIComponent(no),citizen);
assert.equal(page.status,200); assert.equal(page.body.data.totalElements,1);
assert.equal(page.body.data.content[0].requestNo,no);
const simultaneous = await Promise.all(Array.from({length:4},()=>call('/api/relief-requests/'+no+'/allocate',admin,{method:'POST'})));
assert.equal(simultaneous.filter(r=>r.status===200).length,1);
assert.equal(simultaneous.filter(r=>r.status===409).length,3);
assert.equal((await call('/api/relief-requests/'+no,citizen)).body.data.status,'ALLOCATED');
const replayAfterAllocation = await create(citizen,key);
assert.equal(replayAfterAllocation.body.data.requestNo,no);
assert.equal(replayAfterAllocation.body.data.status,'ALLOCATED');
console.log(JSON.stringify({result:'PASS',checks:14,citizenRequest:no,adminRequest:other.body.data.requestNo,reservedWater:1},null,2));
