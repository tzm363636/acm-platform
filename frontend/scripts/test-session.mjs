import assert from 'node:assert/strict'
import { mkdtemp, readFile, writeFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { pathToFileURL } from 'node:url'
import ts from 'typescript'
import { parse, compileScript } from 'vue/compiler-sfc'
import { AxiosError } from 'axios'
import 'vue'

const dir = await mkdtemp(join(tmpdir(), 'acm-session-'))
const events = new EventTarget(), documentEvents = new EventTarget(), stored = new Map()
globalThis.window = events
globalThis.document = Object.assign(documentEvents, { visibilityState: 'visible' })
globalThis.location = new URL('https://fixture.invalid/account.html#/login')
globalThis.localStorage = { setItem: (key, value) => stored.set(key, value) }
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }
const settle = () => new Promise(resolve => setImmediate(resolve))
let checks = 0
const check = (name, fn) => { fn(); console.log('PASS ' + name); checks++ }
try {
  await writeFile(join(dir, 'package.json'), '{"type":"module"}')
  for (const [file, sourcePath] of Object.entries({ errors: 'api/errors', client: 'api/client', database: 'api/database', auth: 'account/auth', identityEpoch: 'account/identityEpoch', leaveGuard: 'account/leaveGuard' })) {
    const source = await readFile(new URL(`../src/${sourcePath}.ts`, import.meta.url), 'utf8')
    const code = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext } }).outputText
      .replaceAll("'./errors'", "'./errors.js'").replaceAll("'./client'", "'./client.js'")
      .replaceAll("'../api/database'", "'./database.js'").replaceAll("'../api/client'", "'./client.js'")
      .replaceAll("'./identityEpoch'", "'./identityEpoch.js'").replaceAll("'../account/identityEpoch'", "'./identityEpoch.js'")
      .replace("'axios'", JSON.stringify(import.meta.resolve('axios'))).replace("'vue'", JSON.stringify(import.meta.resolve('vue')))
      .replace("import.meta.env.VITE_DATA_SOURCE === 'api'", 'true')
    await writeFile(join(dir, `${file}.js`), code)
  }
  const auth = await import(pathToFileURL(join(dir, 'auth.js')))
  const { apiClient, clearCsrf } = await import(pathToFileURL(join(dir, 'client.js')))
  const { getData, postData } = await import(pathToFileURL(join(dir, 'database.js')))
  const guards = await import(pathToFileURL(join(dir, 'leaveGuard.js')))
  const a = { id: 1, username: 'fixture_a', displayName: '用户甲', role: 'USER', createdAt: '2026-10-07T00:00:00Z' }
  const b = { ...a, id: 2, username: 'fixture_b', displayName: '用户乙', role: 'ADMIN' }
  let serverUser = a, meGate, privateGate, csrfGate, failure, calls = []
  const failureResponse = (config, status) => new AxiosError('Isolated fixture failure', 'ERR_BAD_RESPONSE', config, undefined, { status, config, headers: { 'content-type': 'application/json' }, data: { message: status === 401 ? '请先登录。' : status === 403 ? '没有权限。' : '服务暂不可用。' } })
  apiClient.defaults.adapter = async config => {
    calls.push(config.url)
    const response = { status: 200, statusText: 'OK', config, headers: { 'content-type': 'application/json' }, data: {} }
    if (failure?.path === config.url) { const status = failure.status; failure = undefined; throw failureResponse(config, status) }
    if (config.url === '/auth/me') response.data = meGate ? await meGate.promise : { user: serverUser }
    else if (config.url === '/auth/csrf') response.data = csrfGate ? await csrfGate.promise : { token: 'isolated-fixture', headerName: 'X-CSRF-TOKEN' }
    else if (config.url === '/auth/login') response.data = { user: serverUser }
    else if (config.url === '/account/articles' && privateGate) { await privateGate.promise; throw failureResponse(config, 401) }
    return response
  }
  meGate = deferred()
  const initial = auth.loadUser(), joined = auth.loadUser(true)
  await settle()
  check('initial identity is pending and concurrent checks share one request', () => { assert.equal(initial, joined); assert.equal(auth.authStatus.value, 'checking'); assert.equal(calls.length, 1) })
  meGate.resolve({ user: a }); await initial; meGate = undefined
  check('server identity is restored; normal repeated lookup is cached', () => assert.equal(auth.currentUser.value.id, 1))
  const count = calls.length; await auth.loadUser(); assert.equal(calls.length, count)

  meGate = deferred(); const oldIdentity = auth.loadUser(true); await settle()
  serverUser = b; await auth.login('fixture_b', 'test-only-not-a-real-password')
  meGate.resolve({ user: a }); await oldIdentity; meGate = undefined
  check('late identity response cannot overwrite a later login', () => assert.equal(auth.currentUser.value.id, 2))
  privateGate = deferred(); const oldPrivate = getData('/account/articles').catch(e => e); await settle()
  serverUser = a; await auth.login('fixture_a', 'test-only-not-a-real-password')
  privateGate.resolve(); await oldPrivate; privateGate = undefined
  check('late private 401 cannot expire the new login', () => assert.equal(auth.authStatus.value, 'authenticated'))

  failure = { path: '/auth/me', status: 503 }; await auth.loadUser(true)
  check('service outage is unavailable and preserves the last identity', () => { assert.equal(auth.authStatus.value, 'unavailable'); assert.equal(auth.currentUser.value.id, 1) })
  await auth.loadUser(true)
  check('manual identity retry restores authenticated state', () => assert.equal(auth.authStatus.value, 'authenticated'))
  failure = { path: '/account/articles', status: 403 }; await assert.rejects(getData('/account/articles'), e => e.status === 403)
  check('403 does not sign out a valid user', () => assert.equal(auth.authStatus.value, 'authenticated'))
  failure = { path: '/auth/logout', status: 503 }; await assert.rejects(auth.logout(), e => e.status === 503)
  check('failed logout keeps identity, reports outage and ends loading', () => { assert.equal(auth.currentUser.value.id, 1); assert.equal(auth.authBusy.value, false); assert.equal(auth.authStatus.value, 'unavailable') })
  await auth.loadUser(true)
  failure = { path: '/auth/login', status: 401 }; await assert.rejects(auth.login('fixture_a', 'test-only-not-a-real-password'), e => e.status === 401)
  check('invalid login does not masquerade as an unavailable service', () => assert.equal(auth.authStatus.value, 'authenticated'))
  failure = { path: '/auth/login', status: 503 }; await assert.rejects(auth.login('fixture_a', 'test-only-not-a-real-password'), e => e.status === 503)
  check('unavailable login preserves identity and exposes service state', () => { assert.equal(auth.authStatus.value, 'unavailable'); assert.equal(auth.currentUser.value.id, 1) })
  await auth.loadUser(true)
  failure = { path: '/auth/logout', status: 401 }; await assert.rejects(auth.logout(), e => e.status === 401)
  check('logout from an expired server session reports expiry instead of retaining a trusted identity', () => { assert.equal(auth.authStatus.value, 'expired'); assert.equal(auth.currentUser.value, null); assert.equal(auth.authBusy.value, false) })
  await auth.login('fixture_a', 'test-only-not-a-real-password')
  failure = { path: '/account/articles', status: 401 }; await assert.rejects(getData('/account/articles'))
  check('current private 401 expires identity', () => { assert.equal(auth.authStatus.value, 'expired'); assert.equal(auth.currentUser.value, null) })
  await auth.login('fixture_a', 'test-only-not-a-real-password')

  clearCsrf(); csrfGate = deferred(); const writeStart = calls.length
  const blocked = postData('/account/profile', { displayName: '不得发送' }).catch(e => e); await settle()
  auth.expireSession(); csrfGate.resolve({ token: 'isolated-fixture', headerName: 'X-CSRF-TOKEN' }); await blocked; csrfGate = undefined
  check('identity change while fetching CSRF prevents a private write', () => assert.equal(calls.slice(writeStart).includes('/account/profile'), false))
  await auth.login('fixture_a', 'test-only-not-a-real-password')

  meGate = deferred(); const beforeSignal = calls.length
  window.dispatchEvent(Object.assign(new Event('storage'), { key: 'acm-account-refresh' })); await settle()
  window.dispatchEvent(Object.assign(new Event('storage'), { key: 'acm-account-refresh' }))
  check('cross-tab invalidation pauses trusted identity immediately', () => assert.equal(auth.authStatus.value, 'checking'))
  serverUser = b; meGate.resolve({ user: a }); meGate = undefined
  await auth.loadUser(true); await settle()
  check('bursty cross-tab signals coalesce and recover the actual account', () => { assert.equal(auth.currentUser.value.id, 2); assert.equal(calls.slice(beforeSignal).filter(path => path === '/auth/me').length, 2) })
  window.dispatchEvent(Object.assign(new Event('pageshow'), { persisted: true })); await auth.loadUser(true)
  check('history cache restoration revalidates identity', () => assert.equal(auth.authStatus.value, 'authenticated'))
  const beforeFocus = calls.length
  window.dispatchEvent(new Event('focus')); document.dispatchEvent(new Event('visibilitychange')); window.dispatchEvent(new Event('focus')); await auth.loadUser(true)
  check('focus and visibility checks are deduplicated without polling', () => assert.equal(calls.length - beforeFocus, 1))
  serverUser = null
  window.dispatchEvent(Object.assign(new Event('storage'), { key: 'acm-account-refresh' })); await auth.loadUser(true)
  check('another tab logout removes private identity', () => { assert.equal(auth.currentUser.value, null); assert.equal(auth.authStatus.value, 'expired') })
  serverUser = a; await auth.login('fixture_a', 'test-only-not-a-real-password')
  check('only an opaque invalidation signal is persisted', () => { assert.equal(stored.size, 1); assert.match(stored.get('acm-account-refresh'), /^[0-9a-f-]{36}$/) })
  check('return path preserves filters/hash and rejects external redirects', () => {
    assert.equal(auth.safeReturn('/oj.html?q=P1032#/problems?page=2'), '/oj.html?q=P1032#/problems?page=2')
    assert.equal(auth.safeReturn('https://elsewhere.invalid/'), './code-sharing.html')
    assert.equal(auth.safeReturn('/account.html#/login'), './account.html#/articles')
    assert.equal(auth.safeReturn('/\\elsewhere.invalid/'), './code-sharing.html')
  })
  const leave = deferred(); let prompts = 0
  const unregister = guards.registerLeaveGuard(() => { prompts++; return leave.promise })
  const first = guards.requestLeave(), second = guards.requestLeave(); leave.resolve(false)
  check('concurrent leave requests share one decision', () => { assert.equal(first, second); assert.equal(prompts, 1) })
  assert.equal(await first, false); unregister(); assert.equal(await guards.requestLeave(), true)

  // Execute the real form's script with only its route replaced; API calls still use the fixture adapter.
  await writeFile(join(dir, 'route-fixture.js'), "export const useRoute=()=>({path:'/register'});export const useRouter=()=>({replace:async()=>{}})")
  const formSource = await readFile(new URL('../src/account/AuthPage.vue', import.meta.url), 'utf8')
  const compiledForm = compileScript(parse(formSource).descriptor, { id: 'registration-retry' }).content
  const formCode = ts.transpileModule(compiledForm, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext } }).outputText
    .replaceAll("'vue'", JSON.stringify(import.meta.resolve('vue')))
    .replaceAll("'vue-router'", "'./route-fixture.js'").replaceAll("'../api/database'", "'./database.js'")
    .replaceAll("'./auth'", "'./auth.js'").replaceAll("'./PasswordField.vue'", "'./password-fixture.js'")
  await writeFile(join(dir, 'password-fixture.js'), 'export default {}')
  await writeFile(join(dir, 'auth-form.js'), formCode)
  const formComponent = (await import(pathToFileURL(join(dir, 'auth-form.js')))).default
  const form = formComponent.setup({}, { expose() {} })
  let returnDestination
  location.assign = value => { returnDestination = value }
  form.username.value = 'fixture_registration'; form.displayName.value = '专用表单测试'; form.password.value = form.confirmPassword.value = 'test-only-not-a-real-password'
  auth.expireSession(); const beforeRegistration = calls.length
  failure = { path: '/auth/login', status: 503 }; await form.submit()
  check('registration success plus login outage retains input and states which step failed', () => { assert.equal(form.password.value, 'test-only-not-a-real-password'); assert.match(form.error.value, /账户已创建，登录尚未完成/); assert.equal(form.busy.value, false) })
  await form.submit()
  check('retry after successful registration only retries login and clears password on success', () => { assert.equal(calls.slice(beforeRegistration).filter(path => path === '/auth/register').length, 1); assert.equal(form.password.value, ''); assert.equal(form.confirmPassword.value, ''); assert.equal(form.error.value, ''); assert.equal(returnDestination, 'https://fixture.invalid/code-sharing.html') })
  console.log(`${checks} session checks passed`)
} finally {
  await rm(dir, { recursive: true, force: true })
  for (const key of ['window', 'document', 'location', 'localStorage']) delete globalThis[key]
}
