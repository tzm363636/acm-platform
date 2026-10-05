import { createApp } from 'vue'
import { createRouter, createWebHashHistory } from 'vue-router'
import OjApp from './oj/OjApp.vue'
import './styles/main.css'
import './styles/oj.css'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/problems' },
    { path: '/problems', component: () => import('./oj/ProblemList.vue') },
    { path: '/problem/:id', component: () => import('./oj/ProblemDetail.vue') },
    { path: '/submissions', component: () => import('./oj/SubmissionList.vue') },
    { path: '/submission/:id', component: () => import('./oj/SubmissionDetail.vue') },
    { path: '/:pathMatch(.*)*', component: () => import('./oj/NotFound.vue') },
  ],
  scrollBehavior(to, from, saved) {
    if (to.path === from.path) return false
    return saved || { top: 0, behavior: 'instant' }
  },
})
createApp(OjApp).use(router).mount('#app')
