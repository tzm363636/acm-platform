import { apiClient } from './client'
export const databaseMode = import.meta.env.VITE_DATA_SOURCE === 'api'
export interface ServerPage<T> { items: T[]; page: number; pages: number; start: number; end: number; total: number; size: number }
export class ApiError extends Error { constructor(message: string, public status?: number) { super(message) } }
export async function getData<T>(path: string, params: Record<string, unknown> = {}, headers: Record<string,string> = {}): Promise<T> {
  try { return (await apiClient.get<T>(path, { params, headers })).data }
  catch (error: any) { throw new ApiError(error.response?.data?.message || '后端请求失败，请检查服务连接后重试。', error.response?.status) }
}
export async function postData<T>(path: string, body: unknown, headers: Record<string,string> = {}): Promise<T> {
  try { return (await apiClient.post<T>(path, body, { headers })).data }
  catch (error: any) { throw new ApiError(error.response?.data?.message || '后端请求失败，代码和输入已保留，请重试。', error.response?.status) }
}
export async function putData<T>(path: string, body: unknown): Promise<T> {
  try { return (await apiClient.put<T>(path, body)).data }
  catch (error: any) { throw new ApiError(error.response?.data?.message || '保存失败，输入已保留，请重试。', error.response?.status) }
}
