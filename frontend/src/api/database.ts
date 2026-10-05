import { apiClient } from './client'
export const databaseMode = import.meta.env.VITE_DATA_SOURCE === 'api'
export interface ServerPage<T> { items: T[]; page: number; pages: number; start: number; end: number; total: number; size: number }
export async function getData<T>(path: string, params: Record<string, unknown> = {}, headers: Record<string,string> = {}): Promise<T> {
  try { return (await apiClient.get<T>(path, { params, headers })).data }
  catch (error: any) { throw new Error(error.response?.data?.message || '后端请求失败，请检查服务连接后重试。') }
}
export async function postData<T>(path: string, body: unknown, headers: Record<string,string> = {}): Promise<T> {
  try { return (await apiClient.post<T>(path, body, { headers })).data }
  catch (error: any) { throw new Error(error.response?.data?.message || '后端请求失败，代码和输入已保留，请重试。') }
}
