import { readFile, writeFile, mkdir } from 'node:fs/promises'
import ts from 'typescript'
const load = async (path, name) => {
  const source = await readFile(new URL(path, import.meta.url), 'utf8')
  const js = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext } }).outputText
  return (await import(`data:text/javascript;base64,${Buffer.from(js).toString('base64')}`))[name]
}
// Content export only. No credentials, random statistics or simulated AC records.
const articles = await load('../src/data/articles.ts', 'articles')
const problems = await load('../src/oj/problems.ts', 'problems')
const target = new URL('../../backend/src/main/resources/db/seed/site-content.json', import.meta.url)
await mkdir(new URL('.', target), { recursive: true })
await writeFile(target, JSON.stringify({ articles, problems }, null, 2) + '\n')
console.log(`Exported ${articles.length} existing articles and ${problems.length} explicitly labelled demo problems.`)
