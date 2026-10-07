import assert from 'node:assert/strict'
import { mkdtemp, readFile, writeFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { pathToFileURL } from 'node:url'
import ts from 'typescript'
import { AxiosError } from 'axios'

const dir = await mkdtemp(join(tmpdir(), 'acm-api-'))
let checks = 0
const check = (name, fn) => { fn(); console.log('PASS ' + name); checks++ }
globalThis.window = { dispatchEvent: () => {} }
try {
  await writeFile(join(dir, 'package.json'), '{"type":"module"}')
  for (const file of ['errors', 'client', 'database']) {
    const source = await readFile(new URL(`../src/api/${file}.ts`, import.meta.url), 'utf8')
    const code = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext } }).outputText
      .replaceAll("'./errors'", "'./errors.js'").replaceAll("'./client'", "'./client.js'")
      .replace("'axios'", JSON.stringify(import.meta.resolve('axios')))
      .replace("import.meta.env.VITE_DATA_SOURCE === 'api'", 'true')
    await writeFile(join(dir, `${file}.js`), code)
  }
  const { apiClient, clearCsrf } = await import(pathToFileURL(join(dir, 'client.js')))
  const { postData, getData, ApiError } = await import(pathToFileURL(join(dir, 'database.js')))
  const { requestError } = await import(pathToFileURL(join(dir, 'errors.js')))
  let calls = [], scenario = 'valid', rejectWrites = false
  apiClient.defaults.adapter = async config => {
    calls.push({ method: config.method, url: config.url, hasCsrf: !!config.headers.get('X-CSRF-TOKEN') })
    const response = { status: 200, statusText: 'OK', config, headers: { 'content-type': 'application/json;charset=UTF-8' }, data: {} }
    if (config.url === '/auth/csrf') {
      response.data = { token: 'isolated-test-token', headerName: 'X-CSRF-TOKEN' }
      if (scenario === 'html') { response.headers['content-type'] = 'text/html'; response.data = '<!doctype html><html></html>' }
      if (scenario === 'invalid') response.data = { token: '', headerName: 'Other-Header' }
      if (scenario === 'missing') { response.status = 404; response.data = { error: 'Not Found' }; throw new AxiosError('Not Found', 'ERR_BAD_REQUEST', config, undefined, response) }
    } else if (rejectWrites) {
      response.status = 403; response.data = { message: '安全令牌已失效，请重试。', code: 'CSRF_EXPIRED' }
      throw new AxiosError('Forbidden', 'ERR_BAD_REQUEST', config, undefined, response)
    }
    return response
  }
  await Promise.all([postData('/auth/login', {}), postData('/account/profile', {})])
  check('concurrent writes share one CSRF request and attach its header', () => {
    assert.equal(calls.filter(c => c.url === '/auth/csrf').length, 1)
    assert.equal(calls.filter(c => c.method === 'post' && c.hasCsrf).length, 2)
  })
  for (const mode of ['html', 'invalid', 'missing']) {
    clearCsrf(); calls = []; scenario = mode
    await assert.rejects(postData('/auth/login', {}), e => e instanceof ApiError && /API|登录接口/.test(e.message))
    check(`${mode} CSRF response prevents sending a login body`, () => assert.equal(calls.some(c => c.method === 'post'), false))
    scenario = 'valid'; await postData('/auth/login', {})
    check(`${mode} failure can be retried with a fresh token`, () => assert.equal(calls.filter(c => c.url === '/auth/csrf').length, 2))
  }
  rejectWrites = true; calls = []
  await assert.rejects(postData('/account/profile', {}), e => e.status === 403 && /令牌/.test(e.message))
  check('CSRF rejection never automatically replays a write', () => assert.equal(calls.length, 1))
  rejectWrites = false; await postData('/account/profile', {})
  check('explicit retry fetches a new token after expiry', () => assert.equal(calls.filter(c => c.url === '/auth/csrf').length, 1))
  check('server credential and article errors preserve their message/status', () => {
    assert.equal(requestError({ response: { status: 401, data: { message: '用户名或密码错误。' } } }, '/auth/login').message, '用户名或密码错误。')
    assert.equal(requestError({ response: { status: 404, data: { message: '文章不存在。' } } }, '/articles/999').message, '文章不存在。')
  })
  check('timeout, service outage and network failures have distinct feedback', () => {
    assert.match(requestError({ code: 'ECONNABORTED' }, '/auth/me').message, /超时/)
    assert.match(requestError({ response: { status: 503 } }, '/auth/me').message, /暂不可用/)
    assert.match(requestError({ code: 'ERR_NETWORK' }, '/auth/me').message, /无法连接/)
  })
  clearCsrf(); scenario = 'html'
  await assert.rejects(getData('/auth/csrf'), e => e instanceof ApiError && /API/.test(e.message))
  console.log(`${checks} API checks passed`)
} finally { await rm(dir, { recursive: true, force: true }); delete globalThis.window }
