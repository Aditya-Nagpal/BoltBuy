import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    tailwindcss()
  ],
  server: {
    proxy: {
      '/api/v1/orders': {
        target: 'http://localhost:9000',
        changeOrigin: true,
      },
      '/api/v1/worker': {
        target: 'http://localhost:9000',
        changeOrigin: true,
      }
    }
  }
})
