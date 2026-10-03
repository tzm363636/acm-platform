import axios from 'axios'

// 为后续 REST API 预留单一入口；首页目前不依赖后端数据。
export const apiClient = axios.create({ baseURL: '/api', timeout: 8000 })
