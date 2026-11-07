<template>
  <div class="study-container">
    <!-- 左侧：图片展示区 & 作文提交区 -->
    <div class="left-panel">
      <!-- 图片区域 -->
      <div class="image-wrapper">
        <el-image v-if="taskInfo?.imageUrl" class="responsive-image" :src="taskInfo.imageUrl" fit="cover">
          <template #error>
            <div class="image-slot error-slot">
              <el-icon><WarningFilled /></el-icon>
              <span>图片加载失败，请联系老师检查网络或图片链接</span>
            </div>
          </template>
        </el-image>
        <div v-else class="image-slot empty-slot">
          <el-icon><Picture /></el-icon>
          <span>老师未提供此任务的观察图片哦~ 请仔细阅读 AI 老师的文字提示</span>
        </div>
      </div>

      <!-- 最终作文提交区域 -->
      <div class="essay-submit-panel">
        <div class="panel-header">
          <h3>{{ isReviseMode ? '📝 修改我的作文' : '📝 提交我的作文' }}</h3>
          <span class="hint">提交后老师将能看到并批改你的作品哦</span>
        </div>

        <div class="form-content" :class="{ 'is-locked': isSubmitted }">
          <div class="form-item">
            <label>标题</label>
            <el-input v-model="essayForm.title" placeholder="给作文起一个响亮的名字吧" :disabled="isSubmitted" />
          </div>
          <div class="form-item">
            <label>开头</label>
            <el-input v-model="essayForm.opening" type="textarea" :rows="2" placeholder="把我们在开头讨论的内容写下来吧" :disabled="isSubmitted" />
          </div>
          <div class="form-item">
            <label>中间</label>
            <el-input v-model="essayForm.middle" type="textarea" :rows="4" placeholder="移步换景、五感法写出来的段落都在这里啦" :disabled="isSubmitted" />
          </div>
          <div class="form-item">
            <label>结尾</label>
            <el-input v-model="essayForm.ending" type="textarea" :rows="2" placeholder="写出你对老街的感受吧" :disabled="isSubmitted" />
          </div>
          <div class="form-item">
            <label>写作反思与收获 <span style="font-weight: normal; color: #888;">(选填)</span></label>
            <el-input v-model="essayForm.reflection" type="textarea" :rows="2" placeholder="这次写作你遇到了什么困难？或者有什么收获和总结吗？" :disabled="isSubmitted" />
          </div>
        </div>

        <div class="action-buttons">
          <el-button v-if="!isSubmitted" type="warning" plain @click="handleSave">暂存草稿</el-button>
          <el-button v-if="!isSubmitted" type="success" @click="handleSubmit">
            {{ isReviseMode ? '提交修改给老师' : '确认提交给老师' }}
          </el-button>
          <div v-else class="submitted-tip">
            <el-icon color="#4caf50"><CircleCheckFilled /></el-icon>
            已成功提交，等待老师批改
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧：AI 对话区 -->
    <div class="right-panel">
      <div class="chat-header">
        <span>AI 伴学助手</span>
        <div class="header-actions">
          <el-button :type="isTtsEnabled ? 'success' : 'info'" circle @click="toggleTts" :title="isTtsEnabled ? '关闭语音播报' : '开启语音播报'">
            <el-icon v-if="isTtsEnabled"><Bell /></el-icon>
            <el-icon v-else><Mute /></el-icon>
          </el-button>
          <div style="display: flex; align-items: center; margin-right: 15px;">
            <span style="font-size: 12px; color: #666; margin-right: 5px;">语速</span>
            <el-slider v-model="speechRate" :min="0.5" :max="1.5" :step="0.1" size="small" style="width: 80px;" />
          </div>
          <el-button type="danger" plain size="small" @click="clearChat" :disabled="chatHistory.length <= 1 || isLoading || isSubmitted">
            <el-icon><Delete /></el-icon> 清空记录
          </el-button>
          <el-button type="success" plain size="small" @click="exportChat" :disabled="chatHistory.length <= 1">
            <el-icon><Download /></el-icon> 导出记录
          </el-button>
        </div>
      </div>

      <div class="chat-content" ref="chatContentRef">
        <div v-for="(msg, index) in chatHistory" :key="index" :class="['message-bubble', msg.role === 'user' ? 'user-msg' : 'ai-msg']">
          <div class="avatar">{{ msg.role === 'user' ? '学' : 'AI' }}</div>
          <div class="content"><div style="white-space: pre-wrap;">{{ msg.content }}</div></div>
        </div>
        <div v-if="isLoading" class="message-bubble ai-msg">
          <div class="avatar">AI</div>
          <div class="content loading-dots">思考中...</div>
        </div>
      </div>

      <div class="chat-input">
        <el-input v-model="inputText" type="textarea" :rows="3" placeholder="输入你想问的问题，按 Enter 发送..." resize="none" :disabled="isSubmitted" @keydown.enter.prevent="sendMessage" />
        <el-button :type="isRecording ? 'danger' : 'default'" circle @click="toggleRecording" :disabled="isLoading || isRecognizing || isSubmitted" :title="isRecording ? '停止录音' : '语音输入'">
          <el-icon v-if="isRecording"><VideoPause /></el-icon>
          <el-icon v-else><Microphone /></el-icon>
        </el-button>
        <el-button type="primary" class="send-btn" :loading="isLoading || isRecognizing" :disabled="!inputText.trim() || isSubmitted" @click="sendMessage">
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { Microphone, Picture, Download, Delete, VideoPause, Bell, Mute, WarningFilled, CircleCheckFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request.ts'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

const route = useRoute()
const taskId = route.params.taskId
const taskInfo = ref<any>(null)

// 🔥 核心修复：明确 reviseId 的类型，并直接用 number | null 定义
const reviseId: number | null = route.query.reviseId ? Number(route.query.reviseId) : null
const oldSessionId: number | null = route.query.sessionId ? Number(route.query.sessionId) : null
const isReviseMode = ref(reviseId !== null)

const sessionId = ref<number | null>(null)

const INITIAL_MESSAGE = '小朋友，输入“开始作文”，我们一起来完成这篇作文吧！'

const inputText = ref('')
const isLoading = ref(false)
const chatContentRef = ref<HTMLElement | null>(null)
const speechRate = ref(1.0)
const isTtsEnabled = ref(true)

const chatHistory = ref<ChatMessage[]>([
  { role: 'assistant', content: INITIAL_MESSAGE }
])

const parsedInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
const userInfo = ref({
  username: parsedInfo.username || '未知用户',
  name: parsedInfo.name || parsedInfo.username || '未知用户',
  studentId: parsedInfo.studentId || '未知学号'
})

const sessionStartTime = ref<Date>(new Date())

const essayForm = ref({
  title: '',
  opening: '',
  middle: '',
  ending: '',
  reflection: ''
})
const isSubmitted = ref(false)
const hasUnsavedContent = ref(false)

const scrollToBottom = async () => {
  await nextTick()
  if (chatContentRef.value) {
    chatContentRef.value.scrollTop = chatContentRef.value.scrollHeight
  }
}

const handleSave = () => {
  hasUnsavedContent.value = true
  ElMessage.success('草稿已保存！千万别忘了点击提交给老师哦~')
}

const handleSubmit = () => {
  if (!essayForm.value.title || !essayForm.value.opening || !essayForm.value.middle || !essayForm.value.ending) {
    ElMessage.warning('请把标题、开头、中间和结尾都写完后再提交哦！')
    return
  }

  if (!sessionId.value) {
    ElMessage.error('写作会话未初始化，请刷新页面重试')
    return
  }

  ElMessageBox.confirm(
    isReviseMode.value ? '提交后将覆盖之前的内容，且不能再修改了。确定提交吗？' : '提交后作文将直接发送给老师，且不能再修改了。确定要提交吗？',
    '确认提交',
    {
      confirmButtonText: '确定提交',
      cancelButtonText: '我再检查一下',
      type: 'success',
    }
  ).then(async () => {
    try {
      let res
      // 🔥 核心修复：这里直接判断 reviseId 是否存在，不用 .value
      if (isReviseMode.value && reviseId !== null) {
        const fullEssay = `【标题】\n${essayForm.value.title}\n\n【开头】\n${essayForm.value.opening}\n\n【中间】\n${essayForm.value.middle}\n\n【结尾】\n${essayForm.value.ending}`

        res = await request.post('/api/student/essay/revise', null, {
          params: {
            submissionId: reviseId, // 🔥 直接传 number 类型的 reviseId
            revisedEssay: fullEssay,
            revisionReflection: essayForm.value.reflection || '跟AI重新对话后提交'
          }
        })
      } else {
        res = await request.post('/api/student/essay/submit', {
          sessionId: sessionId.value,
          taskId: taskId,
          title: essayForm.value.title,
          opening: essayForm.value.opening,
          middle: essayForm.value.middle,
          ending: essayForm.value.ending,
          reflection: essayForm.value.reflection
        })
      }

      if (res.data.code === 200 || res.data.code === '200') {
        isSubmitted.value = true
        hasUnsavedContent.value = false
        ElMessage.success('太棒了！作文已成功提交给老师。')
      } else {
        ElMessage.error(res.data.msg)
      }
    } catch (e) {
      ElMessage.error('提交失败，请检查网络')
    }
  }).catch(() => {})
}

const beforeUnloadListener = (e: BeforeUnloadEvent) => {
  if (!isSubmitted.value && hasUnsavedContent.value) {
    e.preventDefault()
    e.returnValue = '你的作文还没有提交，下次记得提交。'
    return e.returnValue
  }
}

onBeforeRouteLeave(async (to, from) => {
  if (!isSubmitted.value && hasUnsavedContent.value) {
    try {
      await ElMessageBox.confirm(
        '你的作文还没有提交，下次记得提交哦！确定要离开吗？',
        '提示',
        { confirmButtonText: '确定离开', cancelButtonText: '留下继续写', type: 'warning' }
      )
      return true
    } catch {
      return false
    }
  }
  return true
})

const toggleTts = () => {
  isTtsEnabled.value = !isTtsEnabled.value
  if (!isTtsEnabled.value) {
    if (ttsWs) ttsWs.close()
    if (audioCtx) {
      audioCtx.close()
      audioCtx = null
    }
    ElMessage.info('已关闭 AI 语音播报')
  } else {
    ElMessage.success('已开启 AI 语音播报')
  }
}

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || isLoading.value || isSubmitted.value) return
  if (!sessionId.value) {
    ElMessage.warning('正在初始化写作环境，请稍等片刻再发送')
    return
  }

  chatHistory.value.push({ role: 'user', content: text })
  inputText.value = ''
  isLoading.value = true
  await scrollToBottom()

  try {
    const response = await request.post('/api/chat', {
      taskId: taskId,
      sessionId: sessionId.value,
      userMessage: text,
      history: chatHistory.value.slice(0, -1)
    })

    const aiReply = response.data.data || response.data;
    if (!aiReply || typeof aiReply !== 'string') throw new Error('后端返回数据格式异常')

    chatHistory.value.push({ role: 'assistant', content: aiReply })
    if (isTtsEnabled.value) speakText(aiReply)

  } catch (error: any) {
    console.error('请求失败:', error)
    let errorMsg = 'AI 回复失败，请稍后重试'
    if (error.response && error.response.data && error.response.data.msg) {
      errorMsg = error.response.data.msg
    } else if (error.message.includes('timeout')) {
      errorMsg = '网络请求超时，请检查网络'
    }
    ElMessage.error(errorMsg)

    if (chatHistory.value.length > 1) {
      const lastMsg = chatHistory.value[chatHistory.value.length - 1]
      if (lastMsg && lastMsg.role === 'user') {
        chatHistory.value.pop()
        inputText.value = text
      }
    }
  } finally {
    isLoading.value = false
    await scrollToBottom()
  }
}

const clearChat = () => {
  ElMessageBox.confirm('清空后当前界面的聊天记录将无法恢复。确定要清空吗？', '提示', { confirmButtonText: '确定清空', cancelButtonText: '取消', type: 'warning' })
    .then(() => {
      chatHistory.value = [{ role: 'assistant', content: INITIAL_MESSAGE }]
      sessionStartTime.value = new Date()
      ElMessage.success('聊天记录已清空')
    }).catch(() => {})
}

const exportChat = () => {
  if (chatHistory.value.length <= 1) return

  let fileContent = ''
  chatHistory.value.forEach(msg => {
    const prefix = msg.role === 'user' ? '学生' : 'AI'
    fileContent += `${prefix}：${msg.content}\n`
  })

  const pad = (num: number) => num.toString().padStart(2, '0')
  const date = sessionStartTime.value
  const timeStr = `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}_${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`

  const realName = userInfo.value.name || userInfo.value.username || '学生'
  const studentNo = userInfo.value.studentId || '未知学号'
  const taskTitle = taskInfo.value?.title || '未命名任务'

  const safeTaskTitle = taskTitle.replace(/[\\/:*?"<>|]/g, '_')
  const safeRealName = realName.replace(/[\\/:*?"<>|]/g, '_')

  const fileName = `${safeRealName}_${studentNo}_${safeTaskTitle}_${timeStr}.txt`

  const blob = new Blob([fileContent], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)

  ElMessage.success('聊天记录已导出')
}

let ttsWs: WebSocket | null = null;
let audioCtx: AudioContext | null = null;
const pcmQueue: ArrayBuffer[] = [];
let isPlaying = false;
let nextPlayTime = 0;

const initAudioStream = () => {
  if (ttsWs) { ttsWs.close(); ttsWs = null; }
  if (audioCtx) { audioCtx.close(); audioCtx = null; }
  pcmQueue.length = 0; isPlaying = false; nextPlayTime = 0;
  audioCtx = new AudioContext({ sampleRate: 16000 });
};

const processPcmQueue = () => {
  if (!audioCtx || isPlaying || pcmQueue.length === 0) return;
  const ctx = audioCtx;
  isPlaying = true;
  const chunk = pcmQueue.shift();
  if (!chunk) { isPlaying = false; return; }

  const int16Array = new Int16Array(chunk);
  const float32Array = new Float32Array(int16Array.length);
  for (let i = 0; i < int16Array.length; i++) {
    float32Array[i] = int16Array[i]! / 32768.0;
  }
  const audioBuffer = ctx.createBuffer(1, float32Array.length, 16000);
  audioBuffer.copyToChannel(float32Array, 0);
  const source = ctx.createBufferSource();
  source.buffer = audioBuffer;
  source.connect(ctx.destination);
  const currentTime = ctx.currentTime;
  if (nextPlayTime < currentTime) nextPlayTime = currentTime;
  source.start(nextPlayTime);
  nextPlayTime += audioBuffer.duration;
  source.onended = () => { isPlaying = false; processPcmQueue(); };
};

const speakText = (text: string) => {
  if (!isTtsEnabled.value) return;
  if ('speechSynthesis' in window) window.speechSynthesis.cancel();
  initAudioStream();
  if (audioCtx && audioCtx.state === 'suspended') audioCtx.resume();
  ttsWs = new WebSocket(`ws://localhost:8080/ws/tts?rate=${speechRate.value}`);
  ttsWs.binaryType = 'arraybuffer';
  ttsWs.onopen = () => {
    const cleanText = text ? String(text).replace(/[*#`~\-]/g, '') : '';
    const finalText = cleanText.trim() === '' ? '。' : cleanText;
    ttsWs?.send(finalText);
  };
  ttsWs.onmessage = (event) => {
    if (typeof event.data === 'string' || !isTtsEnabled.value) return;
    pcmQueue.push(event.data as ArrayBuffer);
    processPcmQueue();
  };
  ttsWs.onerror = (e) => console.error('TTS WebSocket 错误', e);
};

const isRecording = ref(false)
const isRecognizing = ref(false)
let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []
let mediaStream: MediaStream | null = null

const toggleRecording = () => { if (isRecording.value) stopRecording(); else startRecording(); }

const startRecording = async () => {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true })
    let mimeType = ''
    if (MediaRecorder.isTypeSupported('audio/webm')) mimeType = 'audio/webm'
    else if (MediaRecorder.isTypeSupported('audio/mp4')) mimeType = 'audio/mp4'
    const options = mimeType ? { mimeType } : {}
    mediaRecorder = new MediaRecorder(mediaStream, options)
    audioChunks = []
    mediaRecorder.ondataavailable = (event) => { if (event.data.size > 0) audioChunks.push(event.data) }
    mediaRecorder.onstop = async () => {
      const actualMimeType = mediaRecorder?.mimeType || 'audio/webm'
      const audioBlob = new Blob(audioChunks, { type: actualMimeType })
      await uploadAudioForRecognition(audioBlob, actualMimeType)
      if (mediaStream) { mediaStream.getTracks().forEach(track => track.stop()); mediaStream = null }
    }
    mediaRecorder.start()
    isRecording.value = true
    ElMessage.info('录音中，请说话...')
  } catch (err) {
    console.error('无法访问麦克风:', err)
    ElMessage.error('无法访问麦克风，请检查浏览器权限设置')
  }
}

const stopRecording = () => {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
    isRecording.value = false
  }
}

const uploadAudioForRecognition = async (audioBlob: Blob, mimeType: string) => {
  isRecognizing.value = true
  const formData = new FormData()
  const ext = mimeType.includes('mp4') ? 'mp4' : 'webm'
  formData.append('file', audioBlob, `recording.${ext}`)
  try {
    const response = await request.post('http://localhost:8080/api/asr/recognize', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (response.data.code === '200' || response.data.code === 200) {
      const recognizedText = response.data.data
      if (recognizedText && recognizedText.trim() !== '') {
        inputText.value = recognizedText
        ElMessage.success('语音识别成功，请确认内容后发送')
      } else {
        ElMessage.warning('没有听清您的声音，请再说一遍')
      }
    } else {
      ElMessage.error(response.data.msg || '语音识别失败')
    }
  } catch (error: any) {
    console.error('语音识别请求失败:', error)
    ElMessage.error('语音识别服务异常，请稍后重试')
  } finally {
    isRecognizing.value = false
  }
}

onMounted(async () => {
  sessionStartTime.value = new Date()
  scrollToBottom()

  if (taskId) {
    try {
      const res = await request.get(`/api/student/task/${taskId}`)
      if (res.data.code === 200 || res.data.code === '200') {
        taskInfo.value = res.data.data
      }

      // 🔥 核心修复：判断 reviseId 和 oldSessionId 是否存在，此处不用 .value
      if (isReviseMode.value && oldSessionId !== null && reviseId !== null) {
        sessionId.value = oldSessionId

        const historyRes = await request.get(`/api/student/session/messages/${oldSessionId}`)
        if (historyRes.data.code === 200 || historyRes.data.code === '200') {
          const historyList = historyRes.data.data
          if (historyList.length > 0) {
            chatHistory.value = historyList
          }
        }

        const essayRes = await request.get(`/api/student/essay/${reviseId}`)
        if (essayRes.data.code === 200 || essayRes.data.code === '200') {
          const oldEssay = essayRes.data.data

          // 🔥 核心修复：如果存在修订版，就把拼好的长文本重新拆解回四个输入框
          if (oldEssay.revisedEssay) {
            const revisedText = oldEssay.revisedEssay;
            // 按换行符拆分
            const lines = revisedText.split('\n');
            let title = '', opening = '', middle = '', ending = '';
            let currentSection = '';

            for (let line of lines) {
              if (line.startsWith('【标题】')) { currentSection = 'title'; continue; }
              if (line.startsWith('【开头】')) { currentSection = 'opening'; continue; }
              if (line.startsWith('【中间】')) { currentSection = 'middle'; continue; }
              if (line.startsWith('【结尾】')) { currentSection = 'ending'; continue; }

              if (currentSection === 'title') title += line + '\n';
              if (currentSection === 'opening') opening += line + '\n';
              if (currentSection === 'middle') middle += line + '\n';
              if (currentSection === 'ending') ending += line + '\n';
            }

            essayForm.value.title = title.trim();
            essayForm.value.opening = opening.trim();
            essayForm.value.middle = middle.trim();
            essayForm.value.ending = ending.trim();
          } else {
            // 如果从未修改过，直接读取初稿内容
            essayForm.value.title = oldEssay.title
            essayForm.value.opening = oldEssay.opening
            essayForm.value.middle = oldEssay.middle
            essayForm.value.ending = oldEssay.ending
          }

          // 重置反思框，让学生写新的感悟
          essayForm.value.reflection = ''
        }
      } else {
        const sessionRes = await request.post('/api/student/session/start', null, {
          params: { taskId: taskId }
        })
        if (sessionRes.data.code === 200 || sessionRes.data.code === '200') {
          sessionId.value = sessionRes.data.data.id
        }
      }
    } catch (e) {
      console.error(e)
      ElMessage.error('初始化写作环境失败')
    }
  }
})

onUnmounted(() => {
  if (ttsWs) ttsWs.close();
  if (audioCtx) { audioCtx.close(); audioCtx = null; }
  window.removeEventListener('beforeunload', beforeUnloadListener)
})
</script>

<style scoped>
/* 样式与你原来完全一样，省略 */
.study-container { display: flex; height: 100vh; background-color: #f0fdf4; box-sizing: border-box; padding: 16px; gap: 16px; overflow: hidden; font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif; }
.left-panel { flex: 1.2; display: flex; flex-direction: column; justify-content: flex-start; gap: 16px; overflow-y: auto; padding-right: 4px; }
.left-panel::-webkit-scrollbar { width: 6px; }
.left-panel::-webkit-scrollbar-thumb { background-color: #c8e6c9; border-radius: 4px; }
.image-wrapper { width: 100%; aspect-ratio: 16 / 9; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); background-color: #e8f5e9; border: 2px solid #c8e6c9; flex-shrink: 0; }
.responsive-image { width: 100%; height: 100%; display: block; }
.image-slot { display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100%; color: #4caf50; font-size: 14px; padding: 20px; text-align: center;}
.essay-submit-panel { background-color: #ffffff; border-radius: 12px; padding: 16px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); border: 1px solid #dcedc8; display: flex; flex-direction: column; gap: 12px; }
.panel-header h3 { margin: 0 0 4px 0; color: #2e7d32; font-size: 16px; }
.panel-header .hint { font-size: 12px; color: #888; }
.form-content { display: flex; flex-direction: column; gap: 10px; }
.form-content.is-locked { opacity: 0.7; }
.form-item { display: flex; flex-direction: column; gap: 4px; }
.form-item label { font-size: 13px; font-weight: bold; color: #4caf50; }
.action-buttons { display: flex; justify-content: flex-end; gap: 12px; margin-top: 8px; border-top: 1px dashed #dcedc8; padding-top: 12px; }
.submitted-tip { display: flex; align-items: center; gap: 6px; color: #4caf50; font-weight: bold; font-size: 14px; }
.right-panel { flex: 1; display: flex; flex-direction: column; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); border: 1px solid #dcedc8; overflow: hidden; }
.chat-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background-color: #e8f5e9; border-bottom: 1px solid #dcedc8; font-weight: bold; color: #2e7d32; font-size: 16px; }
.header-actions { display: flex; gap: 8px; align-items: center; }
.chat-content { flex: 1; padding: 16px; overflow-y: auto; background-color: #fafafa; display: flex; flex-direction: column; gap: 16px; }
.message-bubble { display: flex; gap: 12px; max-width: 90%; }
.message-bubble.user-msg { align-self: flex-end; flex-direction: row-reverse; }
.message-bubble.ai-msg { align-self: flex-start; }
.avatar { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: bold; flex-shrink: 0; }
.user-msg .avatar { background-color: #4caf50; color: white; }
.ai-msg .avatar { background-color: #c8e6c9; color: #2e7d32; }
.content { padding: 10px 14px; border-radius: 8px; font-size: 14px; line-height: 1.6; word-break: break-all; }
.user-msg .content { background-color: #a5d6a7; color: #1b5e20; border-top-right-radius: 0; }
.ai-msg .content { background-color: #ffffff; color: #333333; border: 1px solid #e0e0e0; border-top-left-radius: 0; }
.loading-dots { color: #888; font-style: italic; }
.chat-input { padding: 12px 16px; background-color: #ffffff; border-top: 1px solid #dcedc8; display: flex; gap: 12px; align-items: flex-end; }
.chat-input :deep(.el-textarea__inner) { border-radius: 8px; background-color: #f9fbf9; box-shadow: 0 0 0 1px #dcedc8 inset; }
.chat-input :deep(.el-textarea__inner:focus) { box-shadow: 0 0 0 1px #4caf50 inset; }
.send-btn { height: 72px; background-color: #4caf50; border-color: #4caf50; }
.send-btn:hover { background-color: #43a047; border-color: #43a047; }
.empty-slot { background-color: #f1f8e9; color: #7cb342; }
.empty-slot .el-icon { font-size: 48px; margin-bottom: 10px; }
.error-slot { background-color: #ffebee; color: #e57373; }
.error-slot .el-icon { font-size: 48px; margin-bottom: 10px; }
</style>
