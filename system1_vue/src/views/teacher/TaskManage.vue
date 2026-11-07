<template>
  <div class="page-container">
    <el-card class="green-card">
      <template #header>
        <div class="card-header">
          <h3>📝 我的作文主题</h3>
          <el-button type="success" @click="openThemeDialog">
            <el-icon><Plus /></el-icon> 新建主题
          </el-button>
        </div>
      </template>

      <!-- 主题列表 -->
      <el-table :data="themeList" v-loading="loading" stripe>
        <el-table-column prop="themeName" label="主题名称" width="200">
          <template #default="scope">
            <strong style="color: #2e7d32;">{{ scope.row.themeName }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="teachingObjective" label="教学目的" show-overflow-tooltip />
        <el-table-column prop="teachingGoal" label="教学目标" show-overflow-tooltip />
        <el-table-column label="操作" width="320" align="center">
          <template #default="scope">
            <el-button type="primary" link @click="openTaskDrawer(scope.row)">
              <el-icon><Document /></el-icon> 管理任务
            </el-button>
            <el-button type="warning" link @click="openEditThemeDialog(scope.row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button type="success" link @click="openPublishDialog(scope.row)">
              <el-icon><Share /></el-icon> 发布
            </el-button>
            <el-button type="danger" link @click="deleteTheme(scope.row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container" v-if="total > 0">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          layout="total, prev, pager, next"
          :total="total"
          @current-change="fetchThemes"
        />
      </div>
    </el-card>

    <!-- 创建/编辑主题弹窗 -->
    <el-dialog v-model="themeDialogVisible" :title="isThemeEdit ? '编辑主题' : '新建主题'" width="500px">
      <el-form :model="themeForm" label-width="100px">
        <el-form-item label="主题名称" required>
          <el-input v-model="themeForm.themeName" placeholder="例如：写景类作文" />
        </el-form-item>
        <el-form-item label="教学目的">
          <el-input v-model="themeForm.teachingObjective" type="textarea" :rows="2" placeholder="例如：培养学生观察自然的能力" />
        </el-form-item>
        <el-form-item label="教学目标">
          <el-input v-model="themeForm.teachingGoal" type="textarea" :rows="2" placeholder="例如：掌握移步换景的写作手法" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="themeDialogVisible = false">取消</el-button>
        <el-button type="success" @click="submitTheme">确定</el-button>
      </template>
    </el-dialog>

    <!-- 任务管理抽屉 -->
    <el-drawer v-model="taskDrawerVisible" :title="`${currentTheme?.themeName} - 任务列表`" size="50%">
      <div style="margin-bottom: 20px;">
        <el-button type="primary" @click="openTaskDialog">
          <el-icon><Plus /></el-icon> 发布新任务
        </el-button>
      </div>

      <el-table :data="taskList" v-loading="taskLoading" stripe border>
        <el-table-column prop="title" label="任务标题" width="150" />
        <el-table-column label="AI 提示词" show-overflow-tooltip>
          <template #default="scope">
            <span style="color: #666; font-size: 12px;">{{ scope.row.systemPrompt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="scope">
            <el-button type="primary" link @click="openEditTaskDialog(scope.row)">编辑</el-button>
            <el-button type="danger" link @click="deleteTask(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!taskLoading && taskList.length === 0" description="该主题下还没有发布任务" />
    </el-drawer>

    <!-- 🔥 创建/编辑任务弹窗（加入了标准校验） -->
    <el-dialog v-model="taskDialogVisible" :title="isTaskEdit ? '编辑任务' : '发布新任务'" width="600px" @closed="handleDialogClosed">
      <!-- 绑定 ref 和 rules -->
      <el-form ref="taskFormRef" :model="taskForm" :rules="taskRules" label-width="100px">

        <el-form-item label="任务标题" prop="title">
          <el-input v-model="taskForm.title" placeholder="例如：《古镇老街》" />
        </el-form-item>

        <el-form-item label="图片 URL" prop="imageUrl">
          <el-input v-model="taskForm.imageUrl" placeholder="选填：例如 https://example.com/image.jpg" />
        </el-form-item>

        <el-form-item label="专属提示词" prop="systemPrompt">
          <el-input v-model="taskForm.systemPrompt" type="textarea" :rows="6" placeholder="粘贴该任务专属的AI提示词(Prompt)..." />
        </el-form-item>

      </el-form>
      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="success" @click="submitTask">确认</el-button>
      </template>
    </el-dialog>

    <!-- 发布班级弹窗 -->
    <el-dialog v-model="publishDialogVisible" title="发布主题到班级" width="400px">
      <div style="margin-bottom: 15px; color: #666;">
        选择要将【{{ currentPublishTheme?.themeName }}】发布给哪些班级：
      </div>
      <el-checkbox-group v-model="selectedClassIds" style="display: flex; flex-direction: column; gap: 10px;">
        <el-checkbox v-for="cls in classList" :key="cls.id" :label="cls.id">
          {{ cls.className }} (班级码: {{ cls.classCode }})
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="publishing" @click="submitPublish">确认发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus' // 🔥 引入类型
import { Plus, Document, Edit, Share } from '@element-plus/icons-vue'

// 定义 TS 接口
interface EssayTheme {
  id: number
  themeName: string
  teachingObjective: string
  teachingGoal: string
}

interface TClass {
  id: number
  className: string
  classCode: string
}

interface EssayTask {
  id: number
  themeId: number
  title: string
  imageUrl: string
  systemPrompt: string
}

// 主题相关状态
const themeList = ref<EssayTheme[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const themeDialogVisible = ref(false)

const isThemeEdit = ref(false)
const currentThemeId = ref<number | null>(null)

const themeForm = reactive({
  themeName: '',
  teachingObjective: '',
  teachingGoal: ''
})

// 任务相关状态
const taskDrawerVisible = ref(false)
const currentTheme = ref<EssayTheme | null>(null)
const taskList = ref<EssayTask[]>([])
const taskLoading = ref(false)
const taskDialogVisible = ref(false)

const isTaskEdit = ref(false)
const currentTaskId = ref<number | null>(null)

// 任务表单
const taskForm = reactive({
  themeId: null as number | null,
  title: '',
  imageUrl: '',
  systemPrompt: ''
})

// 🔥 核心升级：任务表单的 Ref 和 校验规则 Rules
const taskFormRef = ref<FormInstance>()
const taskRules = reactive<FormRules>({
  title: [
    { required: true, message: '任务标题不能为空', trigger: 'blur' },
    { min: 2, max: 50, message: '长度需在 2 到 50 个字符之间', trigger: 'blur' }
  ],
  systemPrompt: [
    { required: true, message: 'AI 提示词是系统的大脑，绝对不能为空！', trigger: 'blur' }
  ]
  // imageUrl 不写规则，就是选填
})

// 发布相关状态
const publishDialogVisible = ref(false)
const currentPublishTheme = ref<EssayTheme | null>(null)
const selectedClassIds = ref<number[]>([])
const publishing = ref(false)
const classList = ref<TClass[]>([])

// 获取主题列表
const fetchThemes = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/teacher/task/themes', {
      params: { pageNum: pageNum.value, pageSize: pageSize.value }
    })
    if (res.data.code === 200 || res.data.code === '200') {
      themeList.value = res.data.data.records
      total.value = res.data.data.total
    }
  } finally {
    loading.value = false
  }
}

// 打开新建主题弹窗
const openThemeDialog = () => {
  isThemeEdit.value = false
  currentThemeId.value = null
  themeForm.themeName = ''
  themeForm.teachingObjective = ''
  themeForm.teachingGoal = ''
  themeDialogVisible.value = true
}

// 打开编辑主题弹窗
const openEditThemeDialog = (row: EssayTheme) => {
  isThemeEdit.value = true
  currentThemeId.value = row.id
  themeForm.themeName = row.themeName
  themeForm.teachingObjective = row.teachingObjective
  themeForm.teachingGoal = row.teachingGoal
  themeDialogVisible.value = true
}

// 提交主题
const submitTheme = async () => {
  if (!themeForm.themeName) return ElMessage.warning('请输入主题名称')
  try {
    let res
    if (isThemeEdit.value) {
      res = await request.post(`/api/teacher/task/theme/update/${currentThemeId.value}`, themeForm)
    } else {
      res = await request.post('/api/teacher/task/theme/create', themeForm)
    }

    if (res.data.code === 200 || res.data.code === '200') {
      ElMessage.success(res.data.msg || '操作成功')
      themeDialogVisible.value = false
      fetchThemes()
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    ElMessage.error('网络异常')
  }
}

// 删除主题
const deleteTheme = (id: number) => {
  ElMessageBox.confirm('删除主题将同时清空其下所有任务，确定删除吗？', '警告', { type: 'warning' }).then(async () => {
    ElMessage.info('主题删除接口开发中...')
  })
}

// 打开任务抽屉
const openTaskDrawer = (theme: EssayTheme) => {
  currentTheme.value = theme
  taskDrawerVisible.value = true
  fetchTasks(theme.id)
}

// 获取任务列表
const fetchTasks = async (themeId: number) => {
  taskLoading.value = true
  try {
    const res = await request.get(`/api/teacher/task/list/${themeId}`)
    if (res.data.code === 200 || res.data.code === '200') {
      taskList.value = res.data.data
    }
  } finally {
    taskLoading.value = false
  }
}

// 打开新建任务弹窗
const openTaskDialog = () => {
  isTaskEdit.value = false
  currentTaskId.value = null
  taskForm.themeId = currentTheme.value!.id
  taskForm.title = ''
  taskForm.imageUrl = ''
  taskForm.systemPrompt = ''
  taskDialogVisible.value = true
}

// 打开编辑任务弹窗
const openEditTaskDialog = (row: EssayTask) => {
  isTaskEdit.value = true
  currentTaskId.value = row.id
  taskForm.themeId = row.themeId
  taskForm.title = row.title
  taskForm.imageUrl = row.imageUrl
  taskForm.systemPrompt = row.systemPrompt
  taskDialogVisible.value = true
}

// 弹窗关闭时清除校验红线
const handleDialogClosed = () => {
  taskFormRef.value?.clearValidate()
}

// 🔥 提交任务（使用标准表单校验）
const submitTask = async () => {
  if (!taskFormRef.value) return

  // 触发 Element Plus 的校验
  await taskFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请检查标红的必填项')
      return
    }

    try {
      let res
      if (isTaskEdit.value) {
        res = await request.post(`/api/teacher/task/update/${currentTaskId.value}`, taskForm)
      } else {
        res = await request.post('/api/teacher/task/create', taskForm)
      }

      if (res.data.code === 200 || res.data.code === '200') {
        ElMessage.success(res.data.msg || '操作成功')
        taskDialogVisible.value = false
        fetchTasks(currentTheme.value!.id)
      } else {
        ElMessage.error(res.data.msg)
      }
    } catch (error) {
      ElMessage.error('网络异常')
    }
  })
}

// 删除任务
const deleteTask = (taskId: number) => {
  ElMessageBox.confirm('确定要删除该任务吗？', '提示', { type: 'warning' }).then(async () => {
    const res = await request.delete(`/api/teacher/task/${taskId}`)
    if (res.data.code === 200 || res.data.code === '200') {
      ElMessage.success('删除成功')
      fetchTasks(currentTheme.value!.id)
    }
  })
}

// ================= 🔥 发布逻辑 =================

// 获取教师的所有班级
const fetchTeacherClasses = async () => {
  try {
    const res = await request.get('/api/teacher/class/list', {
      params: { pageNum: 1, pageSize: 999 }
    })
    if (res.data.code === 200 || res.data.code === '200') {
      classList.value = res.data.data.records
    }
  } catch (error) {
    console.error('获取班级失败', error)
  }
}

// 打开发布弹窗
const openPublishDialog = async (row: EssayTheme) => {
  currentPublishTheme.value = row
  publishDialogVisible.value = true
  await fetchTeacherClasses()
  try {
    const res = await request.get(`/api/teacher/task/theme/${row.id}/classes`)
    if (res.data.code === 200 || res.data.code === '200') {
      selectedClassIds.value = res.data.data
    }
  } catch (error) {
    selectedClassIds.value = []
  }
}

// 提交发布
const submitPublish = async () => {
  if (selectedClassIds.value.length === 0) {
    return ElMessage.warning('请至少选择一个班级')
  }
  publishing.value = true
  try {
    const res = await request.post('/api/teacher/task/theme/publish', {
      themeId: currentPublishTheme.value!.id,
      classIds: selectedClassIds.value
    })
    if (res.data.code === 200 || res.data.code === '200') {
      ElMessage.success(res.data.msg || '发布成功')
      publishDialogVisible.value = false
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    ElMessage.error('网络异常')
  } finally {
    publishing.value = false
  }
}

onMounted(() => {
  fetchThemes()
})
</script>

<style scoped>
.page-container { padding: 10px; }
.green-card { border-radius: 12px; border: 1px solid #dcedc8; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header h3 { margin: 0; color: #2e7d32; }
.pagination-container { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
