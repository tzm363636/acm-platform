import axios from 'axios'

// Shared backend entry; credentials never enter the browser. Cloud queries can queue behind the small pool.
export const apiClient = axios.create({ baseURL: '/api', timeout: 45000, withCredentials: true })
let csrf: Promise<{ token: string; headerName: string }> | undefined
export function clearCsrf() { csrf = undefined }
apiClient.interceptors.request.use(async config => {
  if (!['get', 'head', 'options'].includes(config.method || 'get')) {
    csrf ||= apiClient.get('/auth/csrf').then(r => r.data).catch(e => { csrf = undefined; throw e })
    const token = await csrf
    config.headers.set(token.headerName, token.token)
  }
  return config
})
apiClient.interceptors.response.use(r => r, error => {
  if (error.response?.data?.code === 'CSRF_EXPIRED') clearCsrf()
  if (error.response?.status === 401 && !error.config?.url?.startsWith('/auth/')) window.dispatchEvent(new Event('acm-session-expired'))
  return Promise.reject(error)
})
