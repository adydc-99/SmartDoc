import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import { createPinia } from 'pinia'
import 'element-plus/dist/index.css'
import './styles.css'
import App from './App.vue'
import router from './router'
createApp(App).use(createPinia()).use(router).use(ElementPlus).mount('#app')
