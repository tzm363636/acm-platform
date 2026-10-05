import { problems } from './problems'
import { LOCAL_USER, verdicts, type Phase, type PersonalStatus, type Problem, type RunResult, type Scenario, type Submission, type Verdict } from './types'

export const RECORDS_KEY = 'acm-oj-demo-records-v1'
export const DRAFT_PREFIX = 'acm-oj-draft-v1:'
export interface StorageLike { getItem(key: string): string | null; setItem(key: string, value: string): void }
const copy = <T>(value: T): T => JSON.parse(JSON.stringify(value))
const completed = (v: Verdict) => v !== 'Pending' && v !== 'Judging'
const normalize = (value: string) => value.trim().replace(/\s+/g, ' ')

export function personalStatus(id: string, records: Submission[], user = LOCAL_USER): PersonalStatus {
  const attempts = records.filter(s => s.problemId === id && s.user === user && completed(s.verdict) && s.verdict !== 'SystemError')
  return attempts.some(s => s.verdict === 'AC') ? 'passed' : attempts.length ? 'failed' : 'untried'
}
export function practiceStats(records: Submission[], user = LOCAL_USER, now = Date.now()) {
  const passed = new Set(records.filter(s => s.user === user && s.verdict === 'AC').map(s => s.problemId))
  const recent = new Set(records.filter(s => s.user === user && s.verdict === 'AC' && now - Date.parse(s.finishedAt || s.submittedAt) <= 7 * 86400000).map(s => s.problemId))
  return { passed: passed.size, total: problems.length, recent: recent.size, review: problems.filter(p => personalStatus(p.id, records, user) === 'failed').length }
}
export function problemStats(id: string, records: Submission[]) {
  const attempts = records.filter(s => s.problemId === id)
  const judged = attempts.filter(s => completed(s.verdict) && s.verdict !== 'SystemError')
  return { submissions: attempts.length, passRate: judged.length ? 100 * judged.filter(s => s.verdict === 'AC').length / judged.length : null }
}
export function pageSlice<T>(items: T[], page: number, pageSize: number) {
  const pages = Math.max(1, Math.ceil(items.length / pageSize))
  const current = Math.max(1, Math.min(pages, page))
  const start = items.length ? (current - 1) * pageSize + 1 : 0
  return { items: items.slice((current - 1) * pageSize, current * pageSize), page: current, pages, start, end: Math.min(current * pageSize, items.length), total: items.length }
}
function fixtures(): Submission[] {
  const outcomes: Verdict[] = ['AC', 'WA', 'CE', 'TLE', 'MLE', 'RE', 'AC', 'SystemError', 'Pending', 'Judging']
  return outcomes.map((verdict, index) => ({
    id: `DEMO-F${String(index + 1).padStart(4, '0')}`, problemId: problems[index % problems.length]!.id,
    user: index % 2 ? '演示 Bob' : '演示 Alice', language: 'cpp17', verdict, phase: completed(verdict) ? 'finished' : verdict === 'Pending' ? 'waiting' : 'judging',
    submittedAt: new Date(Date.UTC(2026, 9, 1, 6, index)).toISOString(),
    finishedAt: completed(verdict) ? new Date(Date.UTC(2026, 9, 1, 6, index, 1)).toISOString() : undefined,
    timeMs: verdict === 'TLE' ? problems[index % problems.length]!.timeLimit + 1 : ['AC', 'WA', 'RE', 'MLE'].includes(verdict) ? 12 : null,
    memoryMB: verdict === 'MLE' ? problems[index % problems.length]!.memoryLimit + 1 : ['AC', 'WA', 'RE', 'TLE'].includes(verdict) ? 2.1 : null,
    information: '预置演示快照，仅用于展示状态；没有执行代码。等待／评测中快照不会自动变成真实评测。其他演示用户的源码不可访问。',
    scenario: completed(verdict) ? verdict as Submission['scenario'] : 'WA', origin: 'fixture', cases: [],
  }))
}
export function createDemoApi(storage?: StorageLike, delayMs = 180) {
  let notice = ''
  let local: Submission[] = []
  try {
    const raw = storage?.getItem(RECORDS_KEY)
    if (raw) {
      const parsed: unknown = JSON.parse(raw)
      if (!Array.isArray(parsed) || !parsed.every(s => s && typeof s.id === 'string' && s.id.startsWith('DEMO-L') && problems.some(p => p.id === s.problemId) && s.user === LOCAL_USER && s.language === 'cpp17' && typeof s.code === 'string' && Object.hasOwn(verdicts, s.verdict))) throw new Error('Invalid cache')
      local = parsed
      for (const record of local) if (!completed(record.verdict)) {
        record.verdict = 'SystemError'; record.phase = 'finished'; record.finishedAt = new Date().toISOString()
        record.information = '演示提交在页面刷新或关闭时中断。没有完成评测，请返回题目重新演示提交。'; record.cases = []; record.timeMs = null; record.memoryMB = null
      }
    }
  } catch { notice = '本地演示记录无法读取；已使用空的本机记录。已有题库不受影响。' }
  const pause = () => new Promise<void>(resolve => setTimeout(resolve, delayMs))
  function persist(next: Submission[]) {
    try { storage?.setItem(RECORDS_KEY, JSON.stringify(next)) }
    catch { notice = '浏览器无法保存演示记录；本次记录只保留在当前页面会话中。' }
    if (!storage) notice = '本地存储不可用；草稿和演示记录不能跨刷新保留。'
    local = copy(next)
  }
  if (local.length) persist(local)
  const peek = () => copy([...fixtures(), ...local]).map(s => {
    if (s.user !== LOCAL_USER) delete s.code
    return s
  })
  const messages: Record<Submission['scenario'], string> = {
    AC: '固定 AC 演示场景已完成。没有实际执行、编译或评测用户代码。',
    WA: '固定 WA 演示场景：输出与答案不同。此信息不是对用户代码的判断。',
    CE: "固定编译错误演示（未编译用户代码）：\nmain.cpp:12:5: error: expected ';' before 'return'\n   12 |     return 0;\n      |     ^",
    RE: '固定运行错误演示（未执行用户代码）：\nRuntime error: simulated abnormal termination.',
    TLE: '固定超时演示：模拟达到时间限制；未执行用户代码。', MLE: '固定内存超限演示：未执行用户代码。',
    SystemError: '固定判题服务异常演示：服务不可用。演示流程已结束，可以重试。',
  }
  return {
    mode: 'demo' as const, peek, get notice() { return notice },
    async listProblems(fail = false) { await pause(); if (fail) throw new Error('模拟网络失败，题库未加载。请重试。'); return copy(problems) },
    async listSubmissions(fail = false) { await pause(); if (fail) throw new Error('模拟网络失败，提交记录未加载。请重试。'); return peek() },
    async getProblem(id: string, fail = false) { return (await this.listProblems(fail)).find(p => p.id === id) },
    async getSubmission(id: string, fail = false) { return (await this.listSubmissions(fail)).find(s => s.id === id) },
    getDraft(id: string, language: string, fallback: string) {
      try { return storage?.getItem(`${DRAFT_PREFIX}${id}:${language}`) ?? fallback }
      catch { notice = '草稿无法读取；当前使用起始模板。'; return fallback }
    },
    saveDraft(id: string, language: string, code: string) {
      try { if (!storage) throw new Error(); storage.setItem(`${DRAFT_PREFIX}${id}:${language}`, code); return true }
      catch { notice = '草稿保存失败，刷新前请复制代码备份。'; return false }
    },
    async run(problem: Problem, code: string, input: string, scenario: Scenario): Promise<RunResult> {
      if (!code.trim()) throw new Error('代码为空，请先输入代码。')
      await pause()
      if (scenario === 'NetworkError') throw new Error('模拟运行请求失败。代码和输入已保留，请切换场景后重试。')
      const sample = problem.samples.find(s => normalize(s.input) === normalize(input))
      const success = scenario === 'AC'
      return { verdict: scenario, output: success ? (sample || problem.samples[0])!.output : scenario === 'WA' ? '固定演示错误输出' : '',
        expected: sample?.output, information: messages[scenario], matchedSample: !!sample && success,
        timeMs: scenario === 'TLE' ? problem.timeLimit + 1 : ['AC', 'WA', 'RE', 'MLE'].includes(scenario) ? 12 : null,
        memoryMB: scenario === 'MLE' ? problem.memoryLimit + 1 : ['AC', 'WA', 'RE', 'TLE'].includes(scenario) ? 2.1 : null }
    },
    async submit(problemId: string, code: string, scenario: Scenario, update: (s: Submission) => void): Promise<Submission> {
      const problem = problems.find(p => p.id === problemId)
      if (!problem) throw new Error('题目不存在。')
      if (!code.trim()) throw new Error('代码为空，请先输入代码。')
      await pause()
      if (scenario === 'NetworkError') throw new Error('模拟提交请求失败；未创建提交记录。代码与输入已保留，请重试。')
      const record: Submission = { id: `DEMO-L${crypto.randomUUID()}`, problemId, user: LOCAL_USER, language: 'cpp17', verdict: 'Pending', phase: 'waiting',
        submittedAt: new Date().toISOString(), timeMs: null, memoryMB: null, code, information: '本机演示提交，用户代码未执行。', scenario, origin: 'local', cases: [] }
      const publish = (phase: Phase, verdict: Verdict) => {
        record.phase = phase; record.verdict = verdict
        persist([...local.filter(s => s.id !== record.id), record]); update(copy(record))
      }
      publish('waiting', 'Pending'); await pause(); publish('compiling', 'Pending'); await pause()
      if (scenario !== 'CE' && scenario !== 'SystemError') { publish('judging', 'Judging'); await pause() }
      record.finishedAt = new Date().toISOString(); record.information = messages[scenario]
      if (['AC', 'WA', 'TLE', 'MLE', 'RE'].includes(scenario)) { record.timeMs = scenario === 'TLE' ? problem.timeLimit + 1 : 12; record.memoryMB = scenario === 'MLE' ? problem.memoryLimit + 1 : 2.1 }
      if (scenario === 'AC' || scenario === 'WA') record.cases = [{ name: '公开样例结果（固定演示）', verdict: scenario, timeMs: 12, memoryMB: 2.1 }]
      publish('finished', scenario)
      return copy(record)
    },
  }
}
let browserStorage: StorageLike | undefined
try { browserStorage = window.localStorage } catch { /* The adapter falls back to session memory. */ }
let offlineApi: ReturnType<typeof createDemoApi> | undefined
export function getOfflineApi() { return offlineApi ??= createDemoApi(browserStorage) }
