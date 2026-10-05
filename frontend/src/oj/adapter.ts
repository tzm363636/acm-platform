import { getOfflineApi } from './api'
import { databaseApi } from './databaseApi'
import { databaseMode } from '../api/database'
export const ojApi = databaseMode ? {
  ...databaseApi,
  async listProblems(fail = false) { return (await databaseApi.queryProblems({page:1,size:8},fail)).items },
  async listSubmissions(fail = false) { return (await databaseApi.querySubmissions({page:1,size:8},fail)).items },
} : getOfflineApi()
