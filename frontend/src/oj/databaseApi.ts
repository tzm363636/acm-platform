import { getData, postData, type ServerPage } from '../api/database'
import type { Problem, RunResult, Scenario, Submission } from './types'

let cached: Submission[] = []
let session = ''
try { session = localStorage.getItem('acm-oj-db-demo-session') || crypto.randomUUID(); localStorage.setItem('acm-oj-db-demo-session', session) } catch { session = crypto.randomUUID() }
const headers = () => ({ 'X-Demo-Session': session })
const optional = async <T>(path: string): Promise<T | undefined> => {
  try { return await getData<T>(path, {}, headers()) }
  catch (error) { if ((error as Error).message.includes('不存在')) return undefined; throw error }
}
export const databaseApi = {
  mode: 'database-demo' as const,
  peek: () => [...cached],
  notice: '',
  async queryProblems(params: Record<string, unknown>, fail = false) {
    if (fail) throw new Error('模拟网络失败，题库未加载。请重试。')
    return getData<ServerPage<Problem>>('/oj/problems', { ...params, mode: 'demo' }, headers())
  },
  async querySubmissions(params: Record<string, unknown>, fail = false) {
    if (fail) throw new Error('模拟网络失败，提交记录未加载。请重试。')
    const page = await getData<ServerPage<Submission>>('/oj/submissions', { ...params, mode: 'demo' }, headers()); cached = page.items; return page
  },
  options: () => getData<{tags:string[];users:string[]}>('/oj/options'),
  practice: () => getData<{passed:number;total:number;recent:number;review:number|null}>('/oj/practice', { mode:'demo' }, headers()),
  async getProblem(id: string, fail = false) { if(fail) throw new Error('模拟题目加载失败。'); return optional<Problem>(`/oj/problems/${encodeURIComponent(id)}`) },
  async getSubmission(id: string, fail = false) { if(fail) throw new Error('模拟记录加载失败。'); return optional<Submission>(`/oj/submissions/${encodeURIComponent(id)}`) },
  getDraft(id:string, language:string, fallback:string) { try { return localStorage.getItem(`acm-oj-draft-v1:${id}:${language}`) ?? fallback } catch { return fallback } },
  saveDraft(id:string, language:string, code:string) { try { localStorage.setItem(`acm-oj-draft-v1:${id}:${language}`,code); return true } catch { return false } },
  run: (problem:Problem,code:string,input:string,scenario:Scenario) => postData<RunResult>(`/oj/demo/problems/${encodeURIComponent(problem.id)}/run`, {code,input,scenario,language:'cpp17'}, headers()),
  async submit(id:string,code:string,scenario:Scenario,update:(record:Submission)=>void) {
    let record = await postData<Submission>(`/oj/demo/problems/${encodeURIComponent(id)}/submissions`, {code,scenario,language:'cpp17'},headers())
    update(record)
    for(let stage=0;stage<4 && record.phase!=='finished';stage++) {
      record=await postData<Submission>(`/oj/demo/submissions/${encodeURIComponent(record.id)}/advance`,{},headers()); update(record)
    }
    if(record.phase!=='finished')throw new Error('演示流程尚未完成，记录已保存，可查看详情。')
    return record
  },
}
