import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import App from './App.vue'
import router from './router'

const app = createApp(App)

// 1. 注册 Element Plus 的所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 2. 挂载 Pinia、Router 和 Element Plus (只需要挂载一次)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 3. 挂载到 DOM
app.mount('#app')
