<template>
  <div class="page-container">
    <div class="welcome-header">
      <h2>✏️ 我的写作任务</h2>
      <p>在这里完成老师布置的作文，AI 老师会一步步指导你哦！</p>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && themeList.length === 0" description="老师还没有发布任何任务哦，去休息一下吧~" />

      <!-- 遍历主题 -->
      <el-card
        v-for="theme in themeList"
        :key="theme.themeId"
        class="theme-card"
        shadow="hover"
      >
        <template #header>
          <div class="theme-header">
            <div class="theme-title">
              <el-icon class="theme-icon"><Collection /></el-icon>
              <span>{{ theme.themeName }}</span>
            </div>
            <span class="objective" v-if="theme.teachingObjective">目标：{{ theme.teachingObjective }}</span>
          </div>
        </template>

        <!-- 主题下的任务列表 -->
        <el-row :gutter="20">
          <el-col :span="8" v-for="task in theme.tasks" :key="task.id">
            <el-card class="task-card" shadow="hover" @click="goToStudy(task.id)">
              <!-- 🔥 老师修改过任务的红点提示 -->
              <div class="red-dot" v-if="task.isUpdated" title="老师修改了该任务">
                <el-icon><BellFilled /></el-icon> 已更新
              </div>

              <div class="task-cover">
                <!-- 这里的图片先取 task.imageUrl，如果没有就用默认图 -->
                <img :src="task.imageUrl || defaultImg" alt="cover" />
              </div>
              <div class="task-info">
                <h4>{{ task.title }}</h4>
                <el-button type="success" size="small" round style="margin-top: 10px;">
                  开始写作 <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { Collection, BellFilled, ArrowRight } from '@element-plus/icons-vue'

// 默认占位图
const defaultImg = 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?ixlib=rb-4.0.3&auto=format&fit=crop&w=500&q=80'

const router = useRouter()
const loading = ref(false)
// 定义返回的主题数据结构
const themeList = ref<any[]>([])

// 获取我的任务
const fetchMyTasks = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/student/tasks')
    if (res.data.code === 200 || res.data.code === '200') {
      themeList.value = res.data.data
    } else {
      // 🔥 如果是自己注册、没有班级的学生，会走到这里
      ElMessage.warning(res.data.msg || '你还没有加入班级哦')
    }
  } catch (error: any) {
    // 🔥 这里可以给一个非常好看的空状态，提示用户去找老师
    ElMessage.error(error.response?.data?.msg || '获取任务失败')
  } finally {
    loading.value = false
  }
}

// 跳转到写作页面
const goToStudy = (taskId: number) => {
  // 跳转到 Study.vue，并把任务ID带在路由参数里
  router.push(`/student/study/${taskId}`)
}

onMounted(() => {
  fetchMyTasks()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
  background-color: #f0fdf4;
  min-height: calc(100vh - 60px);
}

.welcome-header {
  margin-bottom: 24px;
}
.welcome-header h2 {
  color: #2e7d32;
  margin: 0 0 8px 0;
}
.welcome-header p {
  color: #666;
  margin: 0;
  font-size: 14px;
}

.theme-card {
  margin-bottom: 24px;
  border-radius: 12px;
  border: 1px solid #dcedc8;
}

.theme-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.theme-title {
  display: flex;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
  color: #2e7d32;
}
.theme-icon {
  margin-right: 8px;
  font-size: 20px;
}
.objective {
  font-size: 13px;
  color: #888;
  background-color: #e8f5e9;
  padding: 4px 10px;
  border-radius: 12px;
}

.task-card {
  position: relative;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
  overflow: hidden;
  background-color: #fafafa;
}
.task-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(76, 175, 80, 0.2) !important;
}

/* 🔥 老师修改过任务的红点样式 */
.red-dot {
  position: absolute;
  top: 10px;
  right: 10px;
  background-color: #ff4d4f;
  color: white;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(255, 77, 79, 0.4);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); }
}

.task-cover {
  height: 160px;
  overflow: hidden;
}
.task-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.task-card:hover .task-cover img {
  transform: scale(1.05);
}

.task-info {
  padding: 16px;
  text-align: center;
}
.task-info h4 {
  margin: 0;
  font-size: 16px;
  color: #333;
}
</style>
