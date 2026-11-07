<template>
  <div class="page-container">
    <el-card class="green-card">
      <template #header>
        <div class="card-header">
          <h3>🏫 我的班级管理</h3>
          <el-button type="success" @click="openCreateDialog">
            <el-icon><Plus /></el-icon> 创建新班级
          </el-button>
        </div>
      </template>

      <el-table :data="classList" style="width: 100%" v-loading="loading" stripe>
        <el-table-column prop="className" label="班级名称" min-width="150">
          <template #default="scope">
            <strong style="color: #2e7d32;">{{ scope.row.className }}</strong>
          </template>
        </el-table-column>

        <el-table-column prop="classCode" label="班级码 (发给学生)" width="180">
          <template #default="scope">
            <el-tag size="large" type="success" class="code-tag" @click="copyCode(scope.row.classCode)">
              {{ scope.row.classCode }}
              <el-icon style="margin-left: 5px; cursor: pointer;"><CopyDocument /></el-icon>
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="studentCount" label="人数" width="100" align="center" />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />

        <el-table-column label="操作" width="320" align="center">
          <template #default="scope">
            <el-button type="primary" link @click="openStudentDrawer(scope.row)">
              <el-icon><User /></el-icon> 学生名单
            </el-button>
            <el-button type="warning" link @click="openEditDialog(scope.row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button type="success" link @click="goToGrading(scope.row)">
              批改作文
            </el-button>
            <el-button type="danger" link @click="handleDelete(scope.row.id)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && classList.length === 0" description="你还没有创建班级，快去创建吧！" />

      <div class="pagination-container" v-if="total > 0">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑班级' : '创建新班级'" width="400px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="formData.className" placeholder="例如：四年级一班" />
        </el-form-item>
        <el-form-item label="班级人数" prop="studentCount">
          <el-input-number v-model="formData.studentCount" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="success" :loading="submitting" @click="handleSubmit">
          确认{{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 学生名单抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="`${currentClass?.className} - 学生名单`"
      size="60%"
      destroy-on-close
    >
      <div style="margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center;">
        <div>
          <el-button type="success" plain @click="downloadTemplate">
            <el-icon><Download /></el-icon> 下载导入模板
          </el-button>
          <el-upload
            action="#"
            :show-file-list="false"
            :before-upload="handleBeforeUpload"
            style="display: inline-block; margin-left: 10px;"
          >
            <el-button type="primary" :loading="uploading">
              <el-icon><Upload /></el-icon> 批量导入 Excel
            </el-button>
          </el-upload>

          <!-- 🔥 新增：导出按钮 -->
          <el-button
            type="warning"
            plain
            style="margin-left: 10px;"
            :loading="exporting"
            @click="exportStudents"
          >
            <el-icon><Download /></el-icon> 导出名单信息
          </el-button>
        </div>
        <span style="color: #666; font-size: 14px;">提示：账号为姓名首字母，密码为学号+首字母</span>
      </div>

      <el-table :data="studentList" v-loading="studentLoading" stripe border height="calc(100vh - 200px)">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="gender" label="性别" width="80" align="center" />
        <el-table-column prop="studentId" label="学号" width="150" />
        <el-table-column prop="username" label="登录账号" width="150" />
        <el-table-column label="初始密码" width="150">
          <template #default="scope">
            <el-tag type="warning">{{ scope.row.initPassword || '未记录' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center">
          <template #default="scope">
            <el-button type="danger" link @click="handleDeleteStudent(scope.row.id)">
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-dialog v-model="accountDialogVisible" title="🎉 导入成功，请将账号密码发给学生" width="600px">
      <div style="margin-bottom: 15px; color: #e6a23c;">
        提示：请老师复制保存，后续也可在学生名单中直接查看或导出！
      </div>
      <el-table :data="importedAccounts" border stripe height="400">
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="username" label="登录账号" width="150" />
        <el-table-column prop="password" label="初始密码" width="180" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button link type="primary" @click="copyAccount(scope.row)">复制账号密码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button type="success" @click="accountDialogVisible = false">我已保存，关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request, { uploadRequest } from '../../utils/request'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus, Delete, CopyDocument, User, Download, Upload, Edit } from '@element-plus/icons-vue'

interface TClassInfo {
  id: number
  className: string
  classCode: string
  studentCount: number
  remark: string
}

interface StudentInfo {
  id: number
  name: string
  gender: string
  studentId: string
  username: string
  initPassword?: string
}

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
// 🔥 新增：导出加载状态
const exporting = ref(false)

const classList = ref<TClassInfo[]>([])
const studentList = ref<StudentInfo[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const drawerVisible = ref(false)
const currentClass = ref<TClassInfo | null>(null)
const studentLoading = ref(false)
const uploading = ref(false)

const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const isEdit = ref(false)
const currentEditId = ref<number | null>(null)

const accountDialogVisible = ref(false)
const importedAccounts = ref<any[]>([])

const copyAccount = async (row: any) => {
  const text = `姓名：${row.name}，账号：${row.username}，密码：${row.password}`
  await navigator.clipboard.writeText(text)
  ElMessage.success('已复制到剪贴板')
}

const formData = reactive({
  className: '',
  studentCount: 30,
  remark: ''
})

const rules = {
  className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
  studentCount: [{ required: true, message: '请输入班级人数', trigger: 'blur' }]
}

const fetchClasses = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/teacher/class/list', {
      params: { pageNum: pageNum.value, pageSize: pageSize.value }
    })
    if (res.data.code === '200' || res.data.code === 200) {
      classList.value = res.data.data.records
      total.value = res.data.data.total
    }
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  pageNum.value = 1
  fetchClasses()
}

const handleCurrentChange = (val: number) => {
  pageNum.value = val
  fetchClasses()
}

const openCreateDialog = () => {
  isEdit.value = false
  currentEditId.value = null
  formData.className = ''
  formData.studentCount = 30
  formData.remark = ''
  dialogVisible.value = true
}

const openEditDialog = (row: TClassInfo) => {
  isEdit.value = true
  currentEditId.value = row.id
  formData.className = row.className
  formData.studentCount = row.studentCount
  formData.remark = row.remark
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      let res
      if (isEdit.value) {
        res = await request.post(`/api/teacher/class/update/${currentEditId.value}`, formData)
      } else {
        res = await request.post('/api/teacher/class/create', formData)
      }
      if (res.data.code === '200' || res.data.code === 200) {
        ElMessage.success(res.data.msg || '操作成功')
        dialogVisible.value = false
        fetchClasses()
      } else {
        ElMessage.error(res.data.msg || '操作失败')
      }
    } catch (error: any) {
      ElMessage.error(error.msg || error.message || '网络请求异常')
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定要删除该班级吗？删除后学生将失去关联。', '警告', { type: 'warning' }).then(async () => {
    await request.delete(`/api/teacher/class/${id}`)
    ElMessage.success('删除成功')
    fetchClasses()
  })
}

const copyCode = async (code: string) => {
  await navigator.clipboard.writeText(code)
  ElMessage.success(`班级码 ${code} 已复制`)
}

const openStudentDrawer = (row: TClassInfo) => {
  currentClass.value = row
  drawerVisible.value = true
  fetchStudents(row.id)
}

const fetchStudents = async (classId: number) => {
  studentLoading.value = true
  try {
    const res = await request.get(`/api/teacher/student/list/${classId}`)
    if (res.data.code === '200' || res.data.code === 200) {
      studentList.value = res.data.data
    }
  } finally {
    studentLoading.value = false
  }
}

const handleDeleteStudent = (studentId: number) => {
  ElMessageBox.confirm('确定将该学生移出班级吗？', '提示', { type: 'warning' }).then(async () => {
    await request.delete(`/api/teacher/student/${studentId}?classId=${currentClass.value?.id}`)
    ElMessage.success('移除成功')
    if (currentClass.value) fetchStudents(currentClass.value.id)
    fetchClasses()
  })
}

const downloadTemplate = async () => {
  try {
    const res = await request.get('/api/teacher/student/template', {
      responseType: 'blob'
    })
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '学生导入模板.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error('模板下载失败')
  }
}

// 🔥 新增：导出学生名单逻辑
const exportStudents = async () => {
  if (!currentClass.value) return
  exporting.value = true
  try {
    const res = await request.get(`/api/teacher/student/export/${currentClass.value.id}`, {
      params: { className: currentClass.value.className },
      responseType: 'blob' // 接收文件流
    })

    // 生成下载
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    // 这里的文件名和后端设置的一致
    link.download = `${currentClass.value.className}_学生账号信息.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const handleBeforeUpload = async (file: File) => {
  const isExcel = file.name.endsWith('.xlsx') || file.name.endsWith('.xls');
  if (!isExcel) {
    ElMessage.error('文件格式错误：请下载标准模板并保存为 .xlsx 格式！')
    return false
  }

  const formData = new FormData()
  formData.append('file', file)
  if (currentClass.value) formData.append('classId', currentClass.value.id.toString())

  uploading.value = true
  try {
    const res = await uploadRequest.post('/api/teacher/student/import', formData)
    if (res.data.code === '200' || res.data.code === 200) {
      ElMessage.success(res.data.msg)

      importedAccounts.value = res.data.data || []
      if (importedAccounts.value.length > 0) {
        accountDialogVisible.value = true
      }

      if (currentClass.value) fetchStudents(currentClass.value.id)
      fetchClasses()
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (err) {
    ElMessage.error('导入失败，请检查格式')
  } finally {
    uploading.value = false
  }
  return false
}

const goToGrading = (row: TClassInfo) => {
  router.push({
    path: '/teacher/grading',
    query: {
      classId: row.id,
      className: row.className
    }
  })
}

onMounted(() => {
  fetchClasses()
})
</script>

<style scoped>
.page-container { padding: 10px; }
.green-card { border-radius: 12px; border: 1px solid #dcedc8; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header h3 { margin: 0; color: #2e7d32; }
.code-tag { cursor: pointer; font-family: monospace; font-size: 16px; letter-spacing: 2px; }
.code-tag:hover { background-color: #4caf50; color: white; }
.pagination-container { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
