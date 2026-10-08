import assert from 'node:assert/strict'
import { mkdtemp, readFile, writeFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { pathToFileURL } from 'node:url'
import ts from 'typescript'

const dir = await mkdtemp(join(tmpdir(), 'acm-content-'))
const deferred = () => { let resolve, reject; const promise = new Promise((a,b) => { resolve=a; reject=b }); return {promise,resolve,reject} }
let checks=0
const check=(name,fn)=>{fn();console.log('PASS '+name);checks++}
try {
  const source=await readFile(new URL('../src/composables/publicArticles.ts',import.meta.url),'utf8')
  const code=ts.transpileModule(source,{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.ESNext}}).outputText.replace("'vue'",JSON.stringify(import.meta.resolve('vue')))
  await writeFile(join(dir,'content.mjs'),code)
  const {createPublicFeed,readSharingQuery,sharingSearch,articleTime}=await import(pathToFileURL(join(dir,'content.mjs')))
  const initial=readSharingQuery('')
  const result=(id,total=125)=>({items:total?[{id,title:'测试文章'}]:[],total,page:1,pages:Math.max(1,Math.ceil(total/6)),size:6,start:total?1:0,end:Math.min(total,6),options:{categories:['题解'],tags:['图论'],publishedTotal:125,categoryCount:2}})
  check('legacy parameters and combined URL round trip',()=>{
    const q=readSharingQuery('?q=中文+代码&category=题解&tag=DP&more=1&tags=all&sort=updated&page=18&size=12')
    assert.equal(q.tag,'动态规划');assert.deepEqual(readSharingQuery(sharingSearch(q)),q)
    assert.deepEqual(readSharingQuery('?page=-1&size=10000&sort=sql&category=全部'),initial)
  })
  check('Shanghai time and missing historical timestamps',()=>{
    assert.match(articleTime('2026-10-01T00:00:00Z'),/2026.*10.*01.*08:00/)
    for(const value of [undefined,null,'','invalid'])assert.equal(articleTime(value),'时间未记录')
  })
  const pending=[]
  const feed=createPublicFeed((q,signal)=>{const task=deferred();pending.push({...task,q,signal});return task.promise})
  const first=feed.load(initial)
  check('initial loading has no invented content or statistics',()=>{assert.equal(feed.state.loading,true);assert.equal(feed.data.value,null)})
  pending[0].resolve(result(1));assert.equal(await first,true)
  const mutable={...initial,tag:'图论'}, slow=feed.load(mutable)
  mutable.tag='C++'
  const fast=feed.load(mutable)
  check('new read aborts previous read and snapshots its filters',()=>{assert.equal(pending[1].signal.aborted,true);assert.equal(pending[1].q.tag,'图论');assert.equal(feed.data.value.items[0].id,1)})
  pending[2].resolve(result(3));await fast;pending[1].resolve(result(2));assert.equal(await slow,false)
  check('late response cannot replace last selected result',()=>{assert.equal(feed.data.value.items[0].id,3);assert.equal(readSharingQuery(feed.state.loadedKey).tag,'C++')})
  const failed=feed.load({...initial,q:'new query'});pending[3].reject(new Error('文章加载超时，请稍后重试。'));await failed
  check('failure retains identifiable old results and stops loading without replay',()=>{
    assert.equal(feed.data.value.items[0].id,3);assert.match(feed.state.error,/超时/);assert.equal(feed.state.loading,false);assert.equal(pending.length,4);assert.notEqual(feed.state.loadedKey,feed.state.requestedKey)
  })
  const retry=feed.load({...initial,q:'new query'});pending[4].resolve(result(0,0));await retry
  check('explicit retry can publish a genuine empty result',()=>{assert.equal(feed.data.value.total,0);assert.equal(feed.data.value.items.length,0);assert.equal(feed.state.error,'')})
  const a=feed.load(initial), b=feed.load({...initial,q:'last'})
  pending[6].resolve(result(7));await b;pending[5].reject(new Error('stale failure'));await a
  check('late failure cannot replace current successful feedback',()=>{assert.equal(feed.state.error,'');assert.equal(feed.data.value.items[0].id,7)})
  const incompatible=feed.load(initial);pending[7].resolve({items:[],page:1,total:0});await incompatible
  check('mixed backend/frontend versions fail safely and retain content',()=>{assert.match(feed.state.error,/不兼容/);assert.equal(feed.data.value.items[0].id,7)})
  const final=feed.load(initial);feed.dispose();pending[8].resolve(result(9));assert.equal(await final,false)
  check('unmounted view cannot receive late content',()=>{assert.equal(pending[8].signal.aborted,true);assert.equal(feed.data.value.items[0].id,7)})
  console.log(`${checks} content checks passed`)
} finally { await rm(dir,{recursive:true,force:true}) }
