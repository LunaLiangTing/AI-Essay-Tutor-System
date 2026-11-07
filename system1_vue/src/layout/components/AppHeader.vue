<template>
<!--  顶部与退出逻辑，整合了“安全退出”功能-->
  <div class="header-content">
    <div class="user-info">
      <el-avatar :size="32" style="background-color: #4caf50; margin-right: 10px;">
        {{ userName.charAt(0) }}
      </el-avatar>
      <span class="name">{{ userName }}</span>
      <el-tag size="small" type="success" style="margin-left: 10px;">
        {{ roleName }}
      </el-tag>
    </div>

    <el-divider direction="vertical" />

    <el-button type="danger" link @click="handleLogout">
      <el-icon><SwitchButton /></el-icon>
      退出登录
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { SwitchButton } from '@element-plus/icons-vue'

const router = useRouter()

// 获取用户信息
const userInfoStr = localStorage.getItem('userInfo')
const userInfo = userInfoStr ? JSON.parse(userInfoStr) : { name: '未知用户', role: 'STUDENT' }

const userName = ref(userInfo.name || userInfo.username)
const role = ref(userInfo.role)

// 角色名称映射
const roleName = computed(() => {
  const map: Record<string, string> = {
    'STUDENT': '学生',
    'TEACHER': '教师',
    'SUB_ADMIN': '子管理员',
    'MAIN_ADMIN': '超级管理员'
  }
  return map[role.value] || '用户'
})

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出当前账号吗？', '提示', {
    confirmButtonText: '确定退出',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    ElMessage.success('已安全退出')
    router.push('/login')
  }).catch(() => {})
}
</script>

<style scoped>
.header-content {
  display: flex;
  align-items: center;
}
.user-info {
  display: flex;
  align-items: center;
}
.name {
  font-weight: bold;
  color: #333;
}
</style>
