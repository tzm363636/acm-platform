import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  base: './',
  plugins: [vue(), tailwindcss()],
  build: {
    rollupOptions: {
      input: ['index.html', 'code-sharing.html', 'oj.html', 'about.html', 'milestone.html', 'article.html', 'author.html'],
    },
  },
  server: {
    proxy: {
      '/api': 'http://127.0.0.1:8080',
    },
  },
})
