import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import GenericResource from '../views/GenericResource.vue'
import DeviceMap from '../views/DeviceMap.vue'
import Rules from '../views/Rules.vue'
import VisualScreens from '../views/VisualScreens.vue'
import VideoSquare from '../views/VideoSquare.vue'
import HistoryData from '../views/HistoryData.vue'
import HistoryAlarms from '../views/HistoryAlarms.vue'
import Tasks from '../views/Tasks.vue'
import AiAgent from '../views/AiAgent.vue'
import DevTools from '../views/DevTools.vue'
import { flattenMenus, isSuperAdmin } from '../config/menu'

const readUser = () => {
  try { return JSON.parse(localStorage.getItem('iot_user') || '{}') } catch { return {} }
}

const specialComponents = {
  DeviceMap,
  Rules,
  VisualScreens,
  VideoSquare,
  HistoryData,
  HistoryAlarms,
  Tasks,
  AiAgent,
  DevTools
}

const menuRoutes = flattenMenus().map((item) => ({
  path: item.path,
  name: item.path.replace(/\//g, '-').replace(/^-/, '') || 'dashboard',
  component: item.path === '/dashboard' ? Dashboard : specialComponents[item.special] || GenericResource,
  meta: {
    title: item.title,
    kind: item.kind,
    crumb: item.crumb
  }
}))

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login, meta: { public: true, title: '登录' } },
    {
      path: '/',
      component: MainLayout,
      redirect: '/dashboard',
      children: menuRoutes
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
  ],
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} - 工业物联网智能平台` : '工业物联网智能平台'
  const token = localStorage.getItem('iot_token')
  if (!to.meta.public && !token) return '/login'
  if (to.path === '/login' && token) return '/dashboard'
  if (to.path.startsWith('/system/')) {
    const user = readUser()
    if (!isSuperAdmin(user)) return '/dashboard'
  }
  return true
})

export default router
