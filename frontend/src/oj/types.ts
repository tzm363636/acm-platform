export type Verdict = 'Pending' | 'Judging' | 'AC' | 'WA' | 'TLE' | 'MLE' | 'RE' | 'CE' | 'SystemError'
export type Scenario = Exclude<Verdict, 'Pending' | 'Judging'> | 'NetworkError'
export type PersonalStatus = 'untried' | 'failed' | 'passed'
export type Phase = 'waiting' | 'compiling' | 'judging' | 'finished'
export interface Problem {
  id: string; title: string; difficulty: '简单' | '中等' | '困难'; tags: string[]
  timeLimit: number; memoryLimit: number; source: string; description: string; input: string; output: string
  samples: { input: string; output: string; explanation: string }[]; constraints: string[]; template: string; articleIds: number[]
}
export interface Submission {
  id: string; problemId: string; user: string; language: 'cpp17'; verdict: Verdict; phase: Phase
  submittedAt: string; finishedAt?: string; timeMs: number | null; memoryMB: number | null
  code?: string; information: string; scenario: Exclude<Scenario, 'NetworkError'>; origin: 'fixture' | 'local'
  cases: { name: string; verdict: Verdict; timeMs: number | null; memoryMB: number | null }[]
}
export interface RunResult {
  verdict: Verdict; output: string; expected?: string; information: string; timeMs: number | null; memoryMB: number | null; matchedSample: boolean
}
export const LOCAL_USER = '本机演示用户'
export const verdicts: Record<Verdict, { label: string; icon: string; tone: string }> = {
  Pending: { label: '等待评测', icon: '◷', tone: 'neutral' }, Judging: { label: '评测中', icon: '↻', tone: 'blue' },
  AC: { label: '通过', icon: '✓', tone: 'green' }, WA: { label: '答案错误', icon: '×', tone: 'red' },
  TLE: { label: '时间超限', icon: '◷', tone: 'orange' }, MLE: { label: '内存超限', icon: '▣', tone: 'purple' },
  RE: { label: '运行错误', icon: '!', tone: 'red' }, CE: { label: '编译错误', icon: '</>', tone: 'blue' },
  SystemError: { label: '系统异常', icon: '!', tone: 'orange' },
}
export const scenarios: { value: Scenario; label: string }[] = [
  { value: 'WA', label: 'WA · 固定答案错误场景' }, { value: 'AC', label: 'AC · 固定通过场景' },
  { value: 'CE', label: 'CE · 固定编译错误场景' }, { value: 'RE', label: 'RE · 固定运行错误场景' },
  { value: 'TLE', label: 'TLE · 固定超时场景' }, { value: 'MLE', label: 'MLE · 固定内存超限场景' },
  { value: 'SystemError', label: '系统异常 · 固定服务错误场景' }, { value: 'NetworkError', label: '网络失败 · 请求失败，不生成记录' },
]
