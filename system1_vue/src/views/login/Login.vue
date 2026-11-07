<template>
  <div class="login-container">
    <!-- 左侧装饰区 -->
    <div class="login-left">
      <div class="brand">
        <h1>AI 智能作文辅导系统</h1>
        <p>让每一个孩子都爱上写作</p>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="login-right">
      <div class="login-box">
        <h2 class="title">{{ isLogin ? '欢迎登录' : '注册新账号' }}</h2>

        <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-width="0"
          size="large"
        >
          <!-- 用户名 -->
          <el-form-item prop="username">
            <el-input
              v-model="formData.username"
              placeholder="请输入用户名"
              prefix-icon="User"
            />
          </el-form-item>

          <!-- 真实姓名 (仅注册时显示) -->
          <el-form-item prop="name" v-if="!isLogin">
            <el-input
              v-model="formData.name"
              placeholder="请输入真实姓名（如：张三）"
              prefix-icon="Postcard"
            />
          </el-form-item>

          <!-- 🔥 新增：邀请码 (仅注册子管理员时显示) -->
          <el-form-item prop="inviteCode" v-if="!isLogin && formData.role === 'SUB_ADMIN'">
            <el-input
              v-model="formData.inviteCode"
              placeholder="请输入主管理员提供的邀请码"
              prefix-icon="Ticket"
            />
          </el-form-item>

          <!-- 密码 -->
          <el-form-item prop="password">
            <el-input
              v-model="formData.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <!-- 身份选择 -->
          <el-form-item prop="role">
            <el-radio-group v-model="formData.role" class="role-group">
              <el-radio-button value="STUDENT">学生</el-radio-button>
              <el-radio-button value="TEACHER">教师</el-radio-button>
              <el-radio-button value="SUB_ADMIN">子管理员</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <!-- 验证码 -->
          <el-form-item prop="captcha">
            <div class="captcha-wrapper">
              <el-input
                v-model="formData.captcha"
                placeholder="请输入验证码"
                prefix-icon="Key"
                @keyup.enter="handleSubmit"
              />
              <img
                v-if="captchaImg"
                :src="captchaImg"
                @click="getCaptcha"
                class="captcha-img"
                title="点击刷新"
              />
            </div>
          </el-form-item>

          <!-- 按钮区 -->
          <el-form-item>
            <el-button
              type="primary"
              class="submit-btn"
              :loading="loading"
              @click="handleSubmit"
            >
              {{ isLogin ? '登 录' : '注 册' }}
            </el-button>
          </el-form-item>

          <!-- 切换登录/注册 -->
          <div class="toggle-text">
            <span v-if="isLogin">还没有账号？<a @click="toggleMode">立即注册</a></span>
            <span v-else>已有账号？<a @click="toggleMode">返回登录</a></span>
          </div>
        </el-form>
      </div>
    </div>
  </div>

  <!-- 🔥 首次登录班级验证弹窗 -->
  <el-dialog
    v-model="showVerifyDialog"
    title="📚 加入班级验证"
    width="400px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
  >
    <div style="margin-bottom: 15px; color: #666;">
      老师已将你导入系统，为了进入你的班级，请输入老师下发的【班级邀请码】：
    </div>
    <el-input
      v-model="verifyClassCode"
      placeholder="请输入5位班级邀请码"
      style="margin-bottom: 20px;"
    />
    <template #footer>
      <el-button type="success" :loading="verifyLoading" @click="handleVerifyClass" style="width: 100%;">
        验证并进入系统
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const formRef = ref<FormInstance>()

// 状态控制
const isLogin = ref(true) // 默认登录模式
const loading = ref(false)
const captchaImg = ref('')
const realCaptchaCode = ref('') // 实际开发中验证码校验在后端做，这里为了联调暂时存下来
// 新增状态
const showVerifyDialog = ref(false)
const verifyClassCode = ref('')
const verifyLoading = ref(false)
const tempUserInfo = ref<any>(null) // 临时保存登录信息，验证通过后再存本地

// 表单数据
const formData = reactive({
  username: '',
  password: '',
  name: '',
  role: 'STUDENT',
  captcha: '',
  inviteCode: ''
})

// 表单校验规则
const rules = reactive<FormRules>({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择身份', trigger: 'change' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  inviteCode: [
    {
      required: true,
      message: '请输入邀请码',
      trigger: 'blur',
      // 只有当前选的是子管理员，且处于注册模式时，才触发必填校验
      validator: (rule, value, callback) => {
        if (!isLogin.value && formData.role === 'SUB_ADMIN' && !value) {
          callback(new Error('请输入邀请码'))
        } else {
          callback()
        }
      }
    }
  ]
})

// 获取验证码
const getCaptcha = async () => {
  try {
    const res = await request.get('http://localhost:8080/api/captcha/get')
    // 假设后端直接返回 { img: "base64...", code: "abcd" }
    captchaImg.value = res.data.img
    realCaptchaCode.value = res.data.code
  } catch (error) {
    ElMessage.error('验证码加载失败')
  }
}

// 切换登录/注册模式
const toggleMode = () => {
  isLogin.value = !isLogin.value
  formRef.value?.resetFields()
}

// 提交表单 (登录或注册)
// 处理登录/注册提交
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    // 前端简单校验验证码
    if (formData.captcha.toLowerCase() !== realCaptchaCode.value.toLowerCase()) {
      ElMessage.warning('验证码错误，请重新输入')
      getCaptcha()
      return
    }

    loading.value = true

    try {
      if (isLogin.value) {
        // === 处理登录 ===
        const res = await request.post('http://localhost:8080/api/auth/login', {
          username: formData.username,
          password: formData.password,
          role: formData.role
        })

        if (res.data.code === '200' || res.data.code === 200) {

          // 🔥 核心判断：如果是学生且状态为 0（未激活）
          if (formData.role === 'STUDENT' && res.data.data.status === 0) {
            tempUserInfo.value = res.data.data // 暂存用户数据
            localStorage.setItem('token', res.data.data.token) // 必须存 Token，因为验证接口需要用到
            showVerifyDialog.value = true // 弹出验证框
            ElMessage.info('首次登录，请先验证班级码以加入班级')
          } else {
            // 正常登录流程（老师、管理员、或已激活的学生）
            ElMessage.success('登录成功！')
            localStorage.setItem('token', res.data.data.token)
            localStorage.setItem('userInfo', JSON.stringify(res.data.data))

            // 根据角色跳转不同页面
            if (formData.role === 'STUDENT') {
              router.push('/student/home')
            } else if (formData.role === 'TEACHER') {
              router.push('/teacher/home')
            } else {
              router.push('/admin/home')
            }
          }
        } else {
          ElMessage.error(res.data.msg || '登录失败')
        }
      } else {
        // === 处理注册 ===
        const res = await request.post('http://localhost:8080/api/auth/register', {
          username: formData.username,
          password: formData.password,
          name: formData.name,
          role: formData.role
        })

        if (res.data.code === '200' || res.data.code === 200) {
          ElMessage.success('注册成功，请登录！')
          isLogin.value = true
          formRef.value?.resetFields()
        } else {
          ElMessage.error(res.data.msg || '注册失败')
        }
      }
    } catch (error: any) {
      ElMessage.error(error.response?.data?.msg || '网络请求失败，请稍后再试')
    } finally {
      loading.value = false
      getCaptcha() // 刷新验证码
    }
  })
}

// 🔥 配套方法：处理首次登录弹窗的班级码验证
const handleVerifyClass = async () => {
  if (!verifyClassCode.value.trim()) {
    return ElMessage.warning('请输入班级邀请码')
  }

  verifyLoading.value = true
  try {
    const res = await request.post('http://localhost:8080/api/auth/verify-class', {
      classCode: verifyClassCode.value
    })

    if (res.data.code === '200' || res.data.code === 200) {
      ElMessage.success('验证成功，欢迎加入班级！')

      // 验证通过，将状态改为 1，并正式存入 localStorage
      tempUserInfo.value.status = 1
      localStorage.setItem('userInfo', JSON.stringify(tempUserInfo.value))

      showVerifyDialog.value = false // 关闭弹窗

      // 跳转学生主页
      router.push('/student/home')
    } else {
      ElMessage.error(res.data.msg || '班级码验证失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.msg || '网络请求失败')
  } finally {
    verifyLoading.value = false
  }
}

// 页面加载时获取验证码
onMounted(() => {
  getCaptcha()
})
</script>

<style scoped>
.login-container {
  display: flex;
  height: 100vh;
  background-color: #f0fdf4;
}

/* 左侧装饰 */
.login-left {
  flex: 1.2;
  background: linear-gradient(135deg, #a5d6a7 0%, #4caf50 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
  padding: 40px;
  position: relative;
  overflow: hidden;
}

/* 简单画个装饰圆 */
.login-left::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  top: -100px;
  left: -100px;
}

.brand {
  z-index: 1;
  text-align: center;
}

.brand h1 {
  font-size: 36px;
  margin-bottom: 16px;
  letter-spacing: 2px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}

.brand p {
  font-size: 18px;
  opacity: 0.9;
}

/* 右侧表单 */
.login-right {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.login-box {
  width: 400px;
  background: #ffffff;
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(76, 175, 80, 0.15);
  border: 1px solid #dcedc8;
}

.title {
  text-align: center;
  color: #2e7d32;
  margin-bottom: 30px;
  font-size: 24px;
  font-weight: bold;
}

/* 输入框样式重置 */
:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #dcedc8 inset;
  background-color: #f9fbf9;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #4caf50 inset !important;
}

:deep(.el-input__inner) {
  color: #2e7d32;
}

.role-group {
  width: 100%;
  display: flex;
}

.role-group :deep(.el-radio-button) {
  flex: 1;
}

.role-group :deep(.el-radio-button__inner) {
  width: 100%;
  border-color: #dcedc8;
  color: #666;
}

.role-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: #4caf50;
  border-color: #4caf50;
  box-shadow: -1px 0 0 0 #4caf50;
}

.captcha-wrapper {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #dcedc8;
}

.submit-btn {
  width: 100%;
  background-color: #4caf50;
  border-color: #4caf50;
  font-size: 16px;
  letter-spacing: 4px;
  height: 44px;
  margin-top: 10px;
}

.submit-btn:hover {
  background-color: #43a047;
  border-color: #43a047;
}

.toggle-text {
  text-align: center;
  margin-top: 10px;
  font-size: 14px;
  color: #888;
}

.toggle-text a {
  color: #4caf50;
  cursor: pointer;
  font-weight: bold;
  text-decoration: none;
}

.toggle-text a:hover {
  text-decoration: underline;
}
</style>
