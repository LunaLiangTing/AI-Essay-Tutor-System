import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Index.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/login/Login.vue')
    },
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/student',
      component: Layout,
      redirect: '/student/home',
      children: [
        {
          path: 'home',
          name: 'StudentHome',
          component: () => import('../views/student/StudentHome.vue')
        },
        {
          path: 'essays',
          name: 'MyEssays',
          component: () => import('../views/student/MyEssays.vue')
        },
        {
          path: 'study/:taskId',
          name: 'Study',
          component: () => import('../views/student/Study.vue')
        }
      ]
    },
    {
      path: '/teacher',
      component: Layout,
      redirect: '/teacher/home',
      children: [
        {
          path: 'home',
          name: 'TeacherHome',
          component: () => import('../views/teacher/TeacherHome.vue')
        },
        {
          path: 'classes',
          name: 'ClassManage',
          component: () => import('../views/teacher/ClassManage.vue')
        },
        {
          path: 'tasks',
          name: 'TaskManage',
          component: () => import('../views/teacher/TaskManage.vue')
        },
        {
          path: 'grading',
          name: 'Grading',
          component: () => import('../views/teacher/Grading.vue')
        }
      ]
    },
    {
      path: '/admin',
      component: Layout,
      redirect: '/admin/home',
      children: [
        {
          path: 'home',
          name: 'AdminHome',
          component: () => import('../views/admin/AdminHome.vue')
        },
        {
          path: 'users',
          name: 'UserManage',
          component: () => import('../views/admin/UserManage.vue')
        }
      ]
    }
  ]
})

// 🔥 完善后的路由守卫（Vue Router 4 推荐使用 return 替代 next）
router.beforeEach((to, from) => {
  const token = localStorage.getItem('token')
  const userInfo = localStorage.getItem('userInfo')

  // 1. 如果要去的页面是登录页，直接放行
  if (to.path === '/login') {
    return true
  }

  // 2. 如果要访问内部页面，必须同时有 Token 和 UserInfo
  if (!token || !userInfo) {
    // 清理可能残留的脏数据，防止状态混乱
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')

    // 强制打回登录页
    return '/login'
  }

  // 3. 登录状态正常，放行
  return true
})

export default router
