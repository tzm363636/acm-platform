// Deployment failures must not be confused with invalid credentials or article errors.
export const deploymentMessage = '网站 API 未正确接入，请检查 /api 转发和后端部署配置。输入已保留。'
export class ApiError extends Error {
  constructor(message: string, public status?: number) { super(message); this.name = 'ApiError' }
}
export function requestError(error: unknown, path: string): ApiError {
  if (error instanceof ApiError) return error
  const failure = error as { response?: { status?: number; data?: { message?: unknown } }; config?: { url?: string }; code?: string }
  const status = failure?.response?.status, message = failure?.response?.data?.message
  if (typeof message === 'string' && message.trim()) return new ApiError(message, status)
  if (status === 404 && (path.startsWith('/auth/') || failure?.config?.url?.startsWith('/auth/'))) {
    return new ApiError('登录接口不可用，请检查 /api 转发及后端 Aiven 认证配置。输入已保留。', status)
  }
  if (failure?.code === 'ECONNABORTED' || failure?.code === 'ETIMEDOUT') return new ApiError('后端响应超时，服务可能正在启动。输入已保留，请稍后重试。', status)
  if (status === 502 || status === 503 || status === 504) return new ApiError('后端服务暂不可用。输入已保留，请稍后重试。', status)
  return new ApiError('无法连接后端服务。输入已保留，请检查网络与服务配置后重试。', status)
}
export function csrfToken(data: unknown): { token: string; headerName: string } {
  const value = data as { token?: unknown; headerName?: unknown } | null
  if (!value || typeof value.token !== 'string' || !value.token || value.headerName !== 'X-CSRF-TOKEN') throw new ApiError(deploymentMessage)
  return { token: value.token, headerName: value.headerName }
}
