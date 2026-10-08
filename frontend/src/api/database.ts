import { apiClient } from './client'
import { requestError } from './errors'
export { ApiError } from './errors'
export const databaseMode = import.meta.env.VITE_DATA_SOURCE === 'api'
export interface ServerPage<T> { items: T[]; page: number; pages: number; start: number; end: number; total: number; size: number }
export async function getData<T>(path: string, params: Record<string, unknown> = {}, headers: Record<string,string> = {}, options: { signal?: AbortSignal } = {}): Promise<T> {
  try { return (await apiClient.get<T>(path, { params, headers, signal: options.signal })).data }
  catch (error: unknown) { throw requestError(error, path) }
}
export async function postData<T>(path: string, body: unknown, headers: Record<string,string> = {}): Promise<T> {
  try { return (await apiClient.post<T>(path, body, { headers })).data }
  catch (error: unknown) { throw requestError(error, path) }
}
export async function putData<T>(path: string, body: unknown): Promise<T> {
  try { return (await apiClient.put<T>(path, body)).data }
  catch (error: unknown) { throw requestError(error, path) }
}
