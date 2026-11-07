<template>
<!--  动态菜单，根据 localStorage 里保存的用户角色，展示不同的菜单。-->
  <el-menu
    :default-active="route.path"
    class="sidebar-menu"
    router
    background-color="#ffffff"
    text-color="#666"
    active-text-color="#4caf50"
  >
    <!-- 🔥 动态渲染菜单项 -->
    <el-menu-item
      v-for="item in menuList"
      :key="item.path"
      :index="item.path"
    >
      <el-icon><component :is="item.icon" /></el-icon>
      <span>{{ item.title }}</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Document, EditPen, User, Postcard, DataLine } from '@element-plus/icons-vue'

const route = useRoute()

// 获取当前用户角色 (从登录时存入的本地存储中读取)
const userInfoStr = localStorage.getItem('userInfo')
const userRole = userInfoStr ? JSON.parse(userInfoStr).role : 'STUDENT'

// 菜单配置数据
const menuData = {
  STUDENT: [
    { path: '/student/home', title: '任务大厅', icon: EditPen },
    { path: '/student/essays', title: '我的作文本', icon: Document }
  ],
  TEACHER: [
    { path: '/teacher/home', title: '教师工作台', icon: DataLine },
    { path: '/teacher/classes', title: '班级管理', icon: User },
    { path: '/teacher/tasks', title: '任务发布', icon: Postcard },
    { path: '/teacher/grading', title: '作业批阅', icon: EditPen }
  ],
  ADMIN: [
    { path: '/admin/home', title: '系统概览', icon: DataLine },
    { path: '/admin/users', title: '用户管理', icon: User }
  ]
}

// 根据角色获取对应的菜单
const menuList = computed(() => {
  if (['MAIN_ADMIN', 'SUB_ADMIN'].includes(userRole)) {
    return menuData.ADMIN
  }
  return menuData[userRole as keyof typeof menuData] || menuData.STUDENT
})
</script>

<style scoped>
.sidebar-menu {
  border-right: none;
}
:deep(.el-menu-item.is-active) {
  background-color: #e8f5e9;
  font-weight: bold;
  border-right: 4px solid #4caf50;
}
:deep(.el-menu-item:hover) {
  background-color: #f1f8e9;
}
</style>
