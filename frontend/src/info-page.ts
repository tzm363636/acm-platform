import { createApp } from 'vue'
import InfoPageView from './views/InfoPageView.vue'
import './styles/main.css'
import './styles/info-page.css'

const page = document.body.dataset.page
if (page === 'oj' || page === 'about' || page === 'milestone') {
  createApp(InfoPageView, { page }).mount('#app')
}
