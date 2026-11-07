<template>
  <div class="page-container">
    <el-empty
      v-if="!classId"
      description="请先前往【班级管理】列表，点击对应班级的【批改作文】按钮进入哦~"
    >
      <el-button type="success" @click="router.push('/teacher/classes')">去班级管理</el-button>
    </el-empty>

    <el-card class="green-card" v-else>
      <template #header>
        <div class="card-header">
          <h3>💯 作业批阅中心</h3>
          <span style="color: #2e7d32; font-weight: bold; font-size: 14px;">
            当前班级：{{ className }}
          </span>
        </div>
      </template>

      <el-table :data="submissionList" v-loading="loading" stripe border>
        <el-table-column label="学生信息" width="150">
          <template #default="scope">
            <strong>{{ scope.row.studentName || '未知' }}</strong>
            <span style="color: #888; font-size: 12px;">({{ scope.row.studentNo }})</span>
          </template>
        </el-table-column>
        <el-table-column prop="taskTitle" label="任务主题" width="150" />
        <el-table-column prop="title" label="作文标题" width="150" />
        <el-table-column prop="submitTime" label="提交时间" width="180" />

        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 'SUBMITTED'" type="warning">待批阅</el-tag>
            <el-tag v-else-if="scope.row.status === 'GRADED'" type="success">已通过</el-tag>
            <el-tag v-else-if="scope.row.status === 'NEED_REVISE'" type="danger">已打回重做</el-tag>
            <el-tag v-else-if="scope.row.status === 'REVISED'" type="info">修改待批阅</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center">
          <template #default="scope">
            <el-button type="primary" link @click="openGradeDrawer(scope.row)">
              查看详情并批阅
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 批阅抽屉 -->
    <el-drawer v-model="drawerVisible" title="作文批阅" size="60%">
      <div v-if="currentSubmission.id" class="grade-content">
        <el-row :gutter="20">
          <el-col :span="12">
            <h3 class="section-title">
              📝 学生提交的作文
              <el-button
                v-if="currentSubmission.revisedEssay"
                type="primary"
                size="small"
                plain
                style="margin-left: 15px;"
                @click="openHistoryDrawer"
              >
                查看修改轨迹
              </el-button>
            </h3>
            <div class="essay-box">
              <!-- 🔥 修复点1：动态显示修改次数 -->
              <div style="background: #e8f5e9; padding: 10px; border-radius: 4px; margin-bottom: 15px;" v-if="currentSubmission.revisedEssay">
                <strong style="color: #2e7d32;">🔥 这是学生第 {{ currentSubmission.revisionCount || 1 }} 次修改后的版本</strong>
              </div>

              <template v-if="currentSubmission.revisedEssay">
                <p style="line-height: 1.8; color: #333;">{{ currentSubmission.revisedEssay }}</p>
                <p style="color: #888;"><strong>修改反思：</strong>{{ currentSubmission.revisionReflection || '无' }}</p>
              </template>
              <template v-else>
                <h2>{{ currentSubmission.title }}</h2>
                <p><strong>开头：</strong>{{ currentSubmission.opening }}</p>
                <p><strong>中间：</strong>{{ currentSubmission.middle }}</p>
                <p><strong>结尾：</strong>{{ currentSubmission.ending }}</p>
                <p style="color: #888;"><strong>反思与收获：</strong>{{ currentSubmission.reflection || '无' }}</p>
              </template>
            </div>
          </el-col>
          <el-col :span="12">
            <h3 class="section-title">💬 AI 辅导对话记录</h3>
            <div class="chat-box">
              <div v-for="msg in chatMessages" :key="msg.id" :class="['chat-msg', msg.role === 'user' ? 'user' : 'ai']">
                <strong>{{ msg.role === 'user' ? '学生' : 'AI' }}：</strong>{{ msg.content }}
              </div>
              <el-empty v-if="chatMessages.length === 0" description="暂无聊天记录" />
            </div>
          </el-col>
        </el-row>

        <el-divider />
        <div class="grade-form">
          <h3 class="section-title">📝 教师评价</h3>
          <el-form label-width="100px">
            <el-form-item label="评分 (100)">
              <el-input-number v-model="gradeScore" :min="0" :max="100" />
            </el-form-item>
            <el-form-item label="及格线">
              <el-input-number v-model="passScore" :min="0" :max="100" />
              <span style="margin-left: 10px; color: #888; font-size: 12px;">（低于此分数将打回重做）</span>
            </el-form-item>
            <el-form-item label="强制通过">
              <el-switch v-model="forcePass" active-text="即使未达线也直接通过" />
            </el-form-item>
            <el-form-item label="评语">
              <el-input v-model="gradeComment" type="textarea" :rows="4" placeholder="请给出具体的修改建议或鼓励" />
            </el-form-item>
            <el-form-item>
              <el-button type="success" :loading="submitting" @click="submitGrade">确认批阅</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-drawer>

    <!-- 历史轨迹抽屉 -->
    <el-drawer v-model="historyDrawerVisible" title="学生修改轨迹" size="40%">
      <el-timeline>
        <el-timeline-item
          v-for="(history, index) in historyList"
          :key="history.id"
          :timestamp="'第 ' + history.revisionCount + ' 次修改'"
          placement="top"
          type="success"
        >
          <el-card shadow="hover">
            <p style="line-height: 1.8; white-space: pre-wrap;">{{ history.essayContent }}</p>
            <p style="color: #888; margin-top: 10px;"><strong>当时的反思：</strong>{{ history.revisionReflection || '无' }}</p>
          </el-card>
        </el-timeline-item>
        <el-empty v-if="historyList.length === 0" description="暂无修改记录" />
      </el-timeline>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

// 🔥 修复点2：补充了 revisionCount 字段
interface EssaySubmissionInfo {
  id: number
  studentName: string
  studentNo: string
  taskTitle: string
  title: string
  opening: string
  middle: string
  ending: string
  reflection: string
  revisedEssay: string
  revisionReflection: string
  status: string
  score: number
  teacherComment: string
  sessionId: number
  submitTime: string
  revisionCount: number // 必须加上，否则 TS 读不到
}

// 🔥 修复点3：为历史记录定义强类型
interface HistoryRecord {
  id: number
  submissionId: number
  revisionCount: number
  essayContent: string
  revisionReflection: string
  createdAt: string
}

const route = useRoute()
const router = useRouter()
const classId = route.query.classId
const className = route.query.className || '未命名班级'
const loading = ref(false)
const submissionList = ref<EssaySubmissionInfo[]>([])
const drawerVisible = ref(false)

const historyDrawerVisible = ref(false)
// 🔥 修复点4：强类型替换 any[]
const historyList = ref<HistoryRecord[]>([])

// 🔥 修复点5：补充了 revisionCount: 0
const currentSubmission = ref<EssaySubmissionInfo>({
  id: 0, studentName: '', studentNo: '', taskTitle: '', title: '',
  opening: '', middle: '', ending: '', reflection: '', revisedEssay: '',
  revisionReflection: '', status: '', score: 0, teacherComment: '', sessionId: 0, submitTime: '', revisionCount: 0
})

const chatMessages = ref<any[]>([])
const submitting = ref(false)

const gradeScore = ref(85)
const gradeComment = ref('')
const passScore = ref(60)
const forcePass = ref(false)

const fetchSubmissions = async () => {
  if (!classId) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const res = await request.get('/api/teacher/grading/list', { params: { classId } })
    if (res.data.code === 200 || res.data.code === '200') {
      submissionList.value = res.data.data
    }
  } catch (e) {
    ElMessage.error('获取批阅列表失败')
  } finally {
    loading.value = false
  }
}

const openGradeDrawer = async (row: EssaySubmissionInfo) => {
  currentSubmission.value = row
  drawerVisible.value = true
  gradeScore.value = row.score || 85
  gradeComment.value = row.teacherComment || ''
  passScore.value = 60
  forcePass.value = false

  try {
    const res = await request.get(`/api/teacher/grading/messages/${row.sessionId}`)
    if (res.data.code === 200 || res.data.code === '200') {
      chatMessages.value = res.data.data
    }
  } catch (e) {
    ElMessage.error('获取对话记录失败')
  }
}

const submitGrade = async () => {
  if (!currentSubmission.value.id) return
  if (!gradeComment.value.trim()) {
    ElMessage.warning('请给学生写一句评语吧~')
    return
  }

  submitting.value = true
  try {
    const res = await request.post('/api/teacher/grading/submit', {
      submissionId: currentSubmission.value.id,
      score: gradeScore.value,
      teacherComment: gradeComment.value,
      passScore: passScore.value,
      forcePass: forcePass.value
    })

    if (res.data.code === 200 || res.data.code === '200') {
      ElMessage.success(res.data.msg)
      drawerVisible.value = false
      await fetchSubmissions()
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    ElMessage.error('网络异常，批阅失败')
  } finally {
    submitting.value = false
  }
}

const openHistoryDrawer = async () => {
  if (!currentSubmission.value.id) return
  try {
    const res = await request.get(`/api/teacher/grading/history/${currentSubmission.value.id}`)
    if (res.data.code === 200 || res.data.code === '200') {
      historyList.value = res.data.data
      historyDrawerVisible.value = true
    }
  } catch (e) {
    ElMessage.error('获取历史记录失败')
  }
}

onMounted(async () => {
  await fetchSubmissions()
})
</script>

<style scoped>
.page-container { padding: 10px; }
.green-card { border-radius: 12px; border: 1px solid #dcedc8; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header h3 { margin: 0; color: #2e7d32; }

.grade-content { padding: 10px; }
.section-title { color: #2e7d32; border-left: 4px solid #4caf50; padding-left: 10px; margin-bottom: 15px;}

.essay-box { background: #fafafa; padding: 20px; border-radius: 8px; border: 1px dashed #dcedc8; height: 500px; overflow-y: auto; }
.essay-box h2 { text-align: center; color: #333; margin-bottom: 20px; }
.essay-box p { text-indent: 2em; line-height: 1.8; color: #444; }

.chat-box { background: #f0fdf4; padding: 15px; border-radius: 8px; height: 500px; overflow-y: auto; }
.chat-msg { margin-bottom: 15px; padding: 10px; border-radius: 8px; font-size: 14px; line-height: 1.5; }
.chat-msg.user { background: #e8f5e9; text-align: right; }
.chat-msg.ai { background: #ffffff; border: 1px solid #dcedc8; }
</style>
