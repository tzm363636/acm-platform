import assert from 'node:assert/strict'
import { mkdtemp, readFile, writeFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join, resolve, dirname, basename } from 'node:path'
import { pathToFileURL } from 'node:url'
import { webcrypto } from 'node:crypto'
import ts from 'typescript'

// Compile only the isolated data adapter; tests use memory storage and never touch browser drafts.
const directory = await mkdtemp(join(tmpdir(), 'acm-oj-tests-'))
globalThis.crypto ??= webcrypto
let checks = 0
try {
  await writeFile(join(directory, 'package.json'), '{"type":"module"}')
  for (const name of ['types', 'problems', 'api']) {
    const source = await readFile(new URL(`../src/oj/${name}.ts`, import.meta.url), 'utf8')
    const output = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext } }).outputText.replace(/from '(\.\/[^']+)'/g, "from '$1.js'")
    await writeFile(join(directory, `${name}.js`), output)
  }
  const { createDemoApi, personalStatus, practiceStats, problemStats, pageSlice, RECORDS_KEY } = await import(pathToFileURL(join(directory, 'api.js')))
  const { problems } = await import(pathToFileURL(join(directory, 'problems.js')))
  const values = new Map(); const storage = { getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key, value) }
  const api = createDemoApi(storage, 0)
  const check = (label, callback) => { callback(); checks++; console.log(`PASS ${label}`) }
  const p = problems.find(p => p.id === 'P1032')
  check('problem samples and starting templates are coherent', () => {
    assert.equal(p.samples[0].output, '0 2 3 4 5'); assert.match(p.input, /n、m、s/); assert.match(p.template, /n >> m >> s/)
    for (const problem of problems) { assert.ok(problem.samples.length); assert.match(problem.template, /TODO/); assert.ok(!problem.template.includes('dijkstra(')) }
  })
  const baseline = api.peek().length
  const run = await api.run(p, 'not executable C++', p.samples[0].input, 'AC')
  check('run is a fixed demonstration and never creates a submission', () => { assert.equal(run.output, p.samples[0].output); assert.equal(run.matchedSample, true); assert.equal(api.peek().length, baseline); assert.equal(practiceStats(api.peek()).passed, 0) })
  const custom = await api.run(p, 'arbitrary code', 'custom input without expected output', 'AC')
  check('custom input has no invented expected answer', () => { assert.equal(custom.expected, undefined); assert.equal(custom.matchedSample, false); assert.match(custom.information, /没有实际执行/) })
  await assert.rejects(api.submit(p.id, 'code', 'NetworkError', () => {}), /未创建提交记录/)
  await assert.rejects(api.run(p, '', '', 'AC'), /代码为空/)
  check('failed requests and empty code do not create records', () => { assert.equal(api.peek().length, baseline) })
  const stages = []; const snapshot = '#include <iostream>\nint main() { return 0; }'
  const first = await api.submit(p.id, snapshot, 'AC', record => { stages.push(record.phase); if (record.phase !== 'finished') { assert.equal(record.timeMs, null); assert.equal(record.memoryMB, null) } })
  api.saveDraft(p.id, 'cpp17', 'later draft')
  const second = await api.submit(p.id, 'different code', 'AC', () => {})
  check('submission stages, unique ids and immutable code snapshots', () => { assert.deepEqual(stages, ['waiting', 'compiling', 'judging', 'finished']); assert.notEqual(first.id, second.id); assert.equal(api.peek().find(s => s.id === first.id).code, snapshot); assert.equal(api.getDraft(p.id, 'cpp17', ''), 'later draft') })
  check('practice progress deduplicates accepted problems', () => { assert.equal(practiceStats(api.peek()).passed, 1); assert.equal(personalStatus(p.id, api.peek()), 'passed'); assert.equal(practiceStats(api.peek()).recent, 1); assert.ok(problemStats(p.id, api.peek()).submissions >= 2) })
  const verdicts = ['WA', 'TLE', 'MLE', 'RE', 'CE', 'SystemError']
  for (const verdict of verdicts) {
    const record = await api.submit('P1001', snapshot, verdict, () => {})
    assert.equal(record.phase, 'finished'); assert.equal(record.verdict, verdict); assert.ok(record.finishedAt)
    if (verdict === 'CE' || verdict === 'SystemError') { assert.equal(record.timeMs, null); assert.equal(record.cases.length, 0) }
  }
  check('all terminal results end loading; CE and service error contain no fake test cases', () => { assert.equal(api.peek().length, baseline + 8); assert.equal(personalStatus('P1001', api.peek()), 'failed') })
  const fresh = createDemoApi(storage, 0)
  check('records persist; other users source is excluded', () => { assert.equal(fresh.peek().length, api.peek().length); assert.equal(fresh.peek().find(s => s.id === first.id).code, snapshot); assert.ok(fresh.peek().filter(s => s.origin === 'fixture').every(s => s.code === undefined)) })
  api.saveDraft('P1001', 'cpp17', 'another problem')
  api.saveDraft(p.id, 'test-input', 'sample input')
  check('drafts are isolated by problem and language/input channel', () => { assert.equal(api.getDraft(p.id, 'cpp17', ''), 'later draft'); assert.equal(api.getDraft('P1001', 'cpp17', ''), 'another problem'); assert.equal(api.getDraft(p.id, 'test-input', ''), 'sample input'); assert.equal(api.getDraft(p.id, 'future-language', 'template'), 'template') })
  check('pagination handles empty results and out of range pages', () => { assert.deepEqual(pageSlice([], 5, 4), { items: [], page: 1, pages: 1, start: 0, end: 0, total: 0 }); assert.equal(pageSlice(problems, 99, 4).page, 2); assert.equal(pageSlice(problems, 2, 4).start, 5) })
  const unfinished = api.peek().filter(s => s.origin === 'local').map((s, i) => i === 0 ? { ...s, phase: 'judging', verdict: 'Judging', finishedAt: undefined } : s)
  storage.setItem(RECORDS_KEY, JSON.stringify(unfinished))
  const recovered = createDemoApi(storage, 0).peek().find(s => s.id === first.id)
  check('interrupted simulated submissions recover as explicit errors', () => { assert.equal(recovered.verdict, 'SystemError'); assert.match(recovered.information, /中断/); assert.equal(recovered.code, snapshot) })
  const broken = createDemoApi({ getItem: () => 'invalid json', setItem() {} }, 0)
  check('corrupt storage fails visibly without losing the problem catalogue', () => { assert.ok(broken.notice); assert.equal(broken.peek().filter(s => s.origin === 'local').length, 0) })
  console.log(`${checks} OJ checks passed.`)
} finally {
  if (dirname(resolve(directory)) !== resolve(tmpdir()) || !basename(directory).startsWith('acm-oj-tests-')) throw new Error('Unexpected test directory; cleanup refused.')
  await rm(directory, { recursive: true, force: true })
}
