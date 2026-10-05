import axios from 'axios'

// Shared backend entry; credentials never enter the browser. Cloud queries can queue behind the small pool.
export const apiClient = axios.create({ baseURL: '/api', timeout: 30000 })
