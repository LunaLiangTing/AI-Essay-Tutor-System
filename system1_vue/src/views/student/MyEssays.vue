<template>
  <div class="page-container">
    <div class="welcome-header">
      <h2>📚 我的作文本</h2>
      <p>回顾你的每一次写作，看看老师的评价，不断进步吧！</p>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && essayList.length === 0" description="你还没有提交过作文哦，快去任务大厅看看吧~" />

      <el-row :gutter="20">
        <el-col :span="8" v-for="essay in essayList" :key="essay.id" style="margin-bottom: 20px;">
          <el-card class="essay-card" shadow="hover" :class="{ 'need-revise-card': essay.status === 'NEED_REVISE' }">
            <template #header>
              <div class="essay-header">
                <span class="task-title">{{ essay.taskTitle || '未命名任务' }}</span>
                <el-tag :type="getStatusTag(essay.status).type" size="small">
                  {{ getStatusTag(essay.status).text }}
                </el-tag>
              </div>
            </template>

            <div class="essay-info">
              <h3>{{ essay.title }}</h3>
              <p class="time">
                提交时间：{{ formatTime(essay.submitTime) }}
                <!-- 🔥 动态展示修改次数 -->
                <el-tag v-if="essay.revisionCount && essay.revisionCount > 0" type="warning" size="small" style="margin-left: 10px;">
                  已迭代 {{ essay.revisionCount }} 次
                </el-tag>
              </p>

              <div class="score-area" v-if="essay.status !== 'SUBMITTED'">
                <span>老师评分：</span>
                <span class="score" :style="{ color: essay.status === 'NEED_REVISE' ? '#ff4d4f' : '#4caf50' }">
                  {{ essay.score }}
                </span> 分

                <div v-if="essay.status === 'NEED_REVISE'" style="color: #ff4d4f; font-size: 12px; margin-top: 5px;">
                  ⚠️ 老师认为你需要再次学习并修改后重新提交！
                </div>
              </div>
              <div class="score-area" v-else>
                <span style="color: #888;">等待老师批阅中...</span>
              </div>
            </div>

            <div class="action-area">
              <el-button type="primary" plain @click="openDetailDrawer(essay)">查看详情</el-button>

              <!-- 🔥 统一处理可修改状态，提供直接改和找AI改 -->
              <template v-if="essay.status === 'NEED_REVISE' || essay.status === 'SUBMITTED' || essay.status === 'REVISED' || essay.status === 'GRADED'">
                <el-dropdown trigger="click" @command="(cmd: string) => handleReviseAction(cmd, essay)">
                  <el-button :type="essay.status === 'NEED_REVISE' ? 'danger' : (essay.status === 'GRADED' ? 'success' : 'warning')">
                    {{ essay.status === 'NEED_REVISE' ? '立即重做' : (essay.status === 'SUBMITTED' ? '继续修改' : (essay.status === 'REVISED' ? '再次修改' : '二次修改')) }}
                    <el-icon class="el-icon--right"><arrow-down /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="direct">✍️ 直接文字修改</el-dropdown-item>
                      <el-dropdown-item command="ai">🤖 找 AI 辅导后修改</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailDrawerVisible" title="作文详情" size="50%">
      <div v-if="currentEssay.id" class="detail-content">
        <h2 style="text-align: center; color: #2e7d32;">{{ currentEssay.title }}</h2>

        <div class="essay-text-box">
          <!-- 🔥 修复：如果是修改版，不仅显示修改版，还提示是第几次 -->
          <template v-if="currentEssay.revisedEssay">
            <el-alert
              :title="'这是你修改后的新版作文 (第 ' + (currentEssay.revisionCount || 1) + ' 次迭代)'"
              type="success"
              show-icon
              style="margin-bottom: 15px;"
            />
            <p>{{ currentEssay.revisedEssay }}</p>
            <p style="color: #888; margin-top: 15px;"><strong>修改反思：</strong>{{ currentEssay.revisionReflection }}</p>
          </template>
          <template v-else>
            <p><strong>开头：</strong>{{ currentEssay.opening }}</p>
            <p><strong>中间：</strong>{{ currentEssay.middle }}</p>
            <p><strong>结尾：</strong>{{ currentEssay.ending }}</p>
            <p style="color: #888;"><strong>我的反思：</strong>{{ currentEssay.reflection || '无' }}</p>
          </template>
        </div>

        <template v-if="currentEssay.status !== 'SUBMITTED'">
          <el-divider content-position="left">老师的评价</el-divider>
          <el-card class="teacher-comment-card">
            <p><strong>评分：</strong><span style="color: red; font-size: 20px; font-weight: bold;">{{ currentEssay.score }}</span> 分</p>
            <p><strong>评语：</strong>{{ currentEssay.teacherComment }}</p>
          </el-card>
        </template>
      </div>
    </el-drawer>

    <!-- 直接修改抽屉 -->
    <el-drawer v-model="reviseDrawerVisible" title="修改我的作文" size="50%">
      <div v-if="currentEssay.id" class="revise-content">
        <el-alert :title="'老师评语：' + (currentEssay.teacherComment || '暂未批阅')" type="warning" :closable="false" show-icon style="margin-bottom: 20px;">
          <div>当前评分：{{ currentEssay.score }} 分</div>
        </el-alert>

        <el-form label-position="top">
          <el-form-item label="修改后的完整作文">
            <el-input v-model="reviseForm.revisedEssay" type="textarea" :rows="12" placeholder="结合老师的建议，重新写一遍你的作文吧..." />
          </el-form-item>
          <el-form-item label="这次修改，你有什么新的感悟？(选填)">
            <el-input v-model="reviseForm.revisionReflection" type="textarea" :rows="3" placeholder="例如：我明白了原来移步换景的顺序不能乱..." />
          </el-form-item>
          <el-form-item>
            <el-button type="success" :loading="submitting" @click="submitRevision" style="width: 100%;">提交修改给老师</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'

interface EssaySubmission {
  id: number
  taskId: number
  sessionId: number
  taskTitle: string
  title: string
  opening: string
  middle: string
  ending: string
  reflection: string
  status: string
  score: number
  teacherComment: string
  revisedEssay: string
  revisionReflection: string
  submitTime: string
  revisionCount?: number // 🔥 新增这个字段
}

const router = useRouter()
const loading = ref(false)
const essayList = ref<EssaySubmission[]>([])
const detailDrawerVisible = ref(false)
const reviseDrawerVisible = ref(false)

const currentEssay = ref<EssaySubmission>({
  id: 0, taskId: 0, sessionId: 0, taskTitle: '', title: '', opening: '', middle: '', ending: '',
  reflection: '', status: '', score: 0, teacherComment: '', revisedEssay: '', revisionReflection: '', submitTime: '', revisionCount: 0
})

const submitting = ref(false)

const reviseForm = ref({
  revisedEssay: '',
  revisionReflection: ''
})

const getStatusTag = (status: string): { type: 'success' | 'warning' | 'info' | 'danger'; text: string } => {
  switch (status) {
    case 'SUBMITTED': return { type: 'warning', text: '待批阅' }
    case 'GRADED': return { type: 'success', text: '已通过' }
    case 'NEED_REVISE': return { type: 'danger', text: '需重做' }
    case 'REVISED': return { type: 'info', text: '修改待批阅' }
    default: return { type: 'info', text: '未知' }
  }
}

const formatTime = (timeStr: string) => {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ').substring(0, 19)
}

const fetchEssays = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/student/essays')
    if (res.data.code === 200 || res.data.code === '200') {
      essayList.value = res.data.data
    }
  } catch (e) {
    ElMessage.error('获取作文记录失败')
  } finally {
    loading.value = false
  }
}

const openDetailDrawer = (essay: EssaySubmission) => {
  currentEssay.value = essay
  detailDrawerVisible.value = true
}

const openReviseDrawer = (essay: EssaySubmission) => {
  currentEssay.value = essay
  if (essay.revisedEssay) {
    reviseForm.value.revisedEssay = essay.revisedEssay
  } else {
    reviseForm.value.revisedEssay = `【开头】\n${essay.opening}\n\n【中间】\n${essay.middle}\n\n【结尾】\n${essay.ending}`
  }
  reviseForm.value.revisionReflection = ''
  reviseDrawerVisible.value = true
}

const submitRevision = async () => {
  if (!reviseForm.value.revisedEssay.trim()) return ElMessage.warning('修改后的作文不能为空哦')
  if (!currentEssay.value.id) return

  submitting.value = true
  try {
    const res = await request.post('/api/student/essay/revise', null, {
      params: {
        submissionId: currentEssay.value.id,
        revisedEssay: reviseForm.value.revisedEssay,
        revisionReflection: reviseForm.value.revisionReflection
      }
    })
    if (res.data.code === 200 || res.data.code === '200') {
      ElMessage.success('太棒了！修改已提交给老师。')
      reviseDrawerVisible.value = false
      await fetchEssays()
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (e) {
    ElMessage.error('提交失败，请检查网络')
  } finally {
    submitting.value = false
  }
}

const handleReviseAction = (command: string, essay: EssaySubmission) => {
  if (command === 'direct') {
    openReviseDrawer(essay)
  } else if (command === 'ai') {
    router.push({
      path: `/student/study/${essay.taskId}`,
      query: {
        reviseId: essay.id,
        sessionId: essay.sessionId
      }
    })
  }
}

onMounted(async () => {
  await fetchEssays()
})
</script>

<style scoped>
.page-container { padding: 20px; background-color: #f0fdf4; min-height: calc(100vh - 60px); }
.welcome-header { margin-bottom: 24px; }
.welcome-header h2 { color: #2e7d32; margin: 0 0 8px 0; }
.welcome-header p { color: #666; margin: 0; font-size: 14px; }

.essay-card { border-radius: 12px; border: 1px solid #dcedc8; transition: all 0.3s; }
.essay-card:hover { transform: translateY(-5px); box-shadow: 0 8px 16px rgba(76, 175, 80, 0.15) !important; }

.need-revise-card { border: 2px solid #ff4d4f; animation: pulse-red 2s infinite; }
@keyframes pulse-red {
  0% { box-shadow: 0 0 0 0 rgba(255, 77, 79, 0.4); }
  70% { box-shadow: 0 0 0 10px rgba(255, 77, 79, 0); }
  100% { box-shadow: 0 0 0 0 rgba(255, 77, 79, 0); }
}

.essay-header { display: flex; justify-content: space-between; align-items: center; }
.task-title { font-size: 14px; color: #4caf50; font-weight: bold; }

.essay-info h3 { margin: 0 0 10px 0; color: #333; }
.essay-info .time { font-size: 12px; color: #999; margin-bottom: 15px; }

.score-area { background-color: #f1f8e9; padding: 10px; border-radius: 6px; font-size: 14px; margin-bottom: 15px; }
.score-area .score { font-size: 20px; font-weight: bold; }

.action-area { display: flex; justify-content: space-between; }

.detail-content h2 { margin-bottom: 20px; }
.essay-text-box { background-color: #fafafa; padding: 20px; border-radius: 8px; border: 1px dashed #dcedc8; line-height: 1.8; }
.essay-text-box p { text-indent: 2em; margin-bottom: 10px; color: #444; }

.teacher-comment-card { background-color: #e8f5e9; border: none; }
.teacher-comment-card p { margin: 5px 0; line-height: 1.6; }
</style>
