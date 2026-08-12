import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
export default defineConfig({plugins:[vue()],build:{rollupOptions:{output:{manualChunks:{vue:['vue','vue-router','pinia'],ui:['element-plus'],pdf:['pdfjs-dist'],markdown:['markdown-it','dompurify'],highlight:['highlight.js']}}}},server:{port:3000,proxy:{'/api':'http://localhost:8080'}}})
