import { computed, watch, ref } from 'vue'
import { articles, type Article } from '../data/articles'
import { databaseMode, getData } from './database'
export function useRecommendedArticles(ids:()=>number[]) {
 const fetched=ref<Article[]>([])
 const result=computed(()=>databaseMode?fetched.value:articles.filter(a=>ids().includes(a.id)))
 let request=0
 watch(ids,async values=>{
  if(!databaseMode)return
  const version=++request
  const responses=await Promise.allSettled(values.map(id=>getData<Article>(`/articles/${id}`)))
  if(version===request)fetched.value=responses.filter((value): value is PromiseFulfilledResult<Article>=>value.status==='fulfilled').map(value=>value.value)
 },{immediate:true})
 return result
}
