import assert from 'node:assert/strict'
import { mkdtemp, readFile, writeFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { pathToFileURL } from 'node:url'
import ts from 'typescript'
const dir = await mkdtemp(join(tmpdir(), 'acm-writing-'))
let checks = 0
try {
  await writeFile(join(dir, 'package.json'), '{"type":"module"}')
  for (const [file, source] of [['saver', '../src/account/draftSaver.ts'], ['languages', '../src/components/codeLanguages.ts'], ['signature', '../src/account/contentSignature.ts']]) await writeFile(join(dir, file+'.js'), ts.transpileModule(await readFile(new URL(source, import.meta.url), 'utf8'), { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext } }).outputText.replace("../components/codeLanguages", "./languages.js"))
  const { contentSignature } = await import(pathToFileURL(join(dir, 'signature.js')))
  const { DraftSaver } = await import(pathToFileURL(join(dir,'saver.js'))), { codeLanguage } = await import(pathToFileURL(join(dir,'languages.js')))
  let input='first', acknowledged='', valid=true, auto=true, calls=[], pending=[], states=[], scheduled=new Map(), sequence=0
  const saver = new DraftSaver({ dirty:()=>input!==acknowledged, autoAllowed:()=>auto, capture:()=>valid?input:undefined, send: snapshot=>{calls.push(snapshot);return new Promise((resolve,reject)=>pending.push({resolve,reject}))}, accept:result=>{acknowledged=result}, report:state=>states.push(state), schedule: callback=>{let id=++sequence;scheduled.set(id,callback);return id}, cancel:id=>scheduled.delete(id) })
  const tick=async()=>{await Promise.resolve();await Promise.resolve();await Promise.resolve();await Promise.resolve();await Promise.resolve()}
  const runTimer=async()=>{let fn=[...scheduled.values()][0];scheduled.clear();fn?.();await tick()}
  const check=(name, fn)=>{fn();console.log('PASS '+name);checks++}
  saver.edited();input='latest';saver.edited();await runTimer()
  check('debounce sends only latest snapshot',()=>assert.deepEqual(calls,['latest']))
  input='typed during save';saver.edited();await runTimer()
  check('in-flight save never creates a second writer',()=>assert.equal(calls.length,1))
  pending.shift().resolve('latest');await tick()
  check('old response keeps newer input dirty',()=>{assert.equal(input,'typed during save');assert.equal(acknowledged,'latest');assert.equal(states.at(-1),'unsaved')})
  const flush=saver.flush();await tick();pending.shift().resolve('typed during save');assert.equal(await flush,true)
  check('manual flush coordinates with autosave and latest content',()=>{assert.deepEqual(calls,['latest','typed during save']);assert.equal(scheduled.size,0);assert.equal(states.at(-1),'saved')})
  valid=false;input='invalid';saver.edited();await runTimer()
  check('invalid draft does not send to server',()=>assert.equal(calls.length,2))
  valid=true;input='network failure';saver.edited();await runTimer();pending.shift().reject(new Error('offline'));await tick();saver.edited();await runTimer()
  check('failed save preserves input and stops automatic retry',()=>{assert.equal(input,'network failure');assert.equal(calls.length,3);assert.equal(states.at(-1),'failed')})
  const retry=saver.flush();await tick();pending.shift().resolve('network failure');assert.equal(await retry,true)
  auto=false;input='published';saver.edited();await runTimer()
  check('published and locked modes never autosave',()=>assert.equal(calls.length,4))
  saver.dispose();assert.equal(await saver.flush(),false)
  check('disposed editor cannot send or apply another save',()=>assert.equal(calls.length,4))
  check('legacy C++ and unknown plaintext fallbacks',()=>{assert.equal(codeLanguage(undefined).value,'cpp');assert.equal(codeLanguage('unknown').value,'plaintext');assert.equal(codeLanguage('plaintext').label,'纯文本');assert.equal(codeLanguage('python').label,'Python')})
  check('MySQL JSON key order never causes a repeated save',()=>{ const a={title:'title',summary:'summary',categoryId:1,tagIds:[2,1],sections:[{heading:'heading',level:2,paragraphs:['body'],bullets:[],code:'line\n',codeLanguage:'python'}]}; const b={...a,tagIds:[1,2],sections:[{codeLanguage:'python',code:'line\n',bullets:[],paragraphs:['body'],heading:'heading',level:2}]}; assert.equal(contentSignature(a),contentSignature(b)); assert.notEqual(contentSignature(a),contentSignature({...b,sections:[{...b.sections[0],code:'new line\n'}]})) })
  console.log(`${checks} writing checks passed`)
} finally { await rm(dir,{recursive:true,force:true}) }

