import { createApp } from 'vue'
import { createRouter, createWebHashHistory } from 'vue-router'
import AccountApp from './account/AccountApp.vue'
import AuthPage from './account/AuthPage.vue'
import ArticleList from './account/ArticleList.vue'
import ArticleEditor from './account/ArticleEditor.vue'
import AdminPage from './account/AdminPage.vue'
import ProfilePage from './account/ProfilePage.vue'
import './styles/main.css'
import './styles/experience.css'
import './styles/account.css'
import './styles/article.css'
const router = createRouter({ history: createWebHashHistory(), routes: [
  { path: '/', redirect: '/articles' }, { path: '/login', component: AuthPage }, { path: '/register', component: AuthPage },
  { path: '/profile', component: ProfilePage },
  { path: '/articles', component: ArticleList }, { path: '/articles/new', component: ArticleEditor }, { path: '/articles/:id', component: ArticleEditor },
  { path: '/admin', component: ArticleList, props: { admin: true } }, { path: '/admin/articles/:id', component: ArticleEditor },
  { path: '/admin/users', component: AdminPage, props: { mode: 'users' } }, { path: '/admin/taxonomy', component: AdminPage, props: { mode: 'taxonomy' } },
  { path: '/:pathMatch(.*)*', component: AdminPage, props: { mode: 'notfound' } },
] })
createApp(AccountApp).use(router).mount('#app')
