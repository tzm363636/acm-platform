import axios from 'axios'
import { ApiError, csrfToken, deploymentMessage } from './errors'
import { identityEpoch } from '../account/identityEpoch'
declare module 'axios' { interface InternalAxiosRequestConfig { acmIdentityEpoch?: number } }

// Shared backend entry; credentials never enter the browser. Cloud queries can queue behind the small pool.
export const apiClient = axios.create({ baseURL: '/api', timeout: 45000, withCredentials: true })
const authPaths = new Set(['/auth/me', '/auth/csrf', '/auth/login', '/auth/register', '/auth/logout'])
let csrf: Promise<{ token: string; headerName: string }> | undefined
export function clearCsrf() { csrf = undefined }
apiClient.interceptors.request.use(async config => {
  // A free Render instance can take about a minute to wake. Do not replay authentication writes.
  if (authPaths.has(config.url || '') && config.timeout === apiClient.defaults.timeout) config.timeout = 90000
  const mine = config.acmIdentityEpoch = identityEpoch()
  if (!['get', 'head', 'options'].includes(config.method || 'get')) {
    if (!csrf) {
      const pending = apiClient.get('/auth/csrf').then(r => csrfToken(r.data)).catch(e => { if (csrf === pending) csrf = undefined; throw e })
      csrf = pending
    }
    const token = await csrf
    if (mine !== identityEpoch() && /^\/(account|admin)(\/|$)/.test(config.url || '')) throw new ApiError('登录身份已变化，操作已暂停，输入已保留。')
    config.headers.set(token.headerName, token.token)
  }
  return config
})
apiClient.interceptors.response.use(r => {
  // A static host/SPA fallback can return HTTP 200 with an HTML page instead of an API response.
  if (!String(r.headers['content-type'] || '').toLowerCase().includes('application/json')) throw new ApiError(deploymentMessage, r.status)
  return r
}, error => {
  if (error.config?.acmIdentityEpoch === identityEpoch()) {
    if (error.response?.data?.code === 'CSRF_EXPIRED') clearCsrf()
    if (error.response?.status === 401 && !['/auth/login', '/auth/register', '/auth/csrf'].includes(error.config?.url || '')) window.dispatchEvent(new Event('acm-session-expired'))
  }
  return Promise.reject(error)
})
