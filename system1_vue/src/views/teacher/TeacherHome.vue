<template>
  <div class="dashboard-container">
    <!-- 顶部装饰背景 -->
    <div class="bg-glow"></div>

    <!-- 欢迎横幅 (玻璃拟态) -->
    <div class="welcome-banner">
      <div class="welcome-text">
        <h2>欢迎回来，老师 👋</h2>
        <p>今天也是充满活力的一天，这里是您的教学数据总览。</p>
      </div>
      <el-button type="primary" size="large" round @click="router.push('/teacher/classes')" class="action-btn">
        进入我的班级
      </el-button>
    </div>

    <div v-loading="loading">
      <!-- 1. 核心数据 (KPI 滚动数字) -->
      <el-row :gutter="24" class="kpi-row">
        <el-col :span="6">
          <div class="kpi-card blue-card">
            <div class="kpi-top">
              <span class="kpi-title">班级总数</span>
              <div class="kpi-icon"><el-icon><School /></el-icon></div>
            </div>
            <div class="kpi-value">{{ displayStats.totalClasses }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="kpi-card green-card">
            <div class="kpi-top">
              <span class="kpi-title">学生总人数</span>
              <div class="kpi-icon"><el-icon><User /></el-icon></div>
            </div>
            <div class="kpi-value">{{ displayStats.totalStudents }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="kpi-card orange-card">
            <div class="kpi-top">
              <span class="kpi-title">累计提交作文</span>
              <div class="kpi-icon"><el-icon><Document /></el-icon></div>
            </div>
            <div class="kpi-value">{{ displayStats.totalSubmissions }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="kpi-card red-card">
            <div class="kpi-top">
              <span class="kpi-title">待批阅作业</span>
              <div class="kpi-icon"><el-icon><Bell /></el-icon></div>
            </div>
            <div class="kpi-value">{{ displayStats.pendingReview }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- 2. 图表区 -->
      <el-row :gutter="24" class="chart-row">
        <!-- 左侧：近7天提交趋势折线图 -->
        <el-col :span="16">
          <div class="chart-card">
            <div class="chart-header">
              <span class="chart-title">📈 近七日辅导提交趋势</span>
              <el-tag type="success" effect="plain" round>实时更新</el-tag>
            </div>
            <div ref="lineChartRef" style="height: 350px; width: 100%;"></div>
          </div>
        </el-col>

        <!-- 右侧：作业状态玫瑰图 -->
        <el-col :span="8">
          <div class="chart-card">
            <div class="chart-header">
              <span class="chart-title">🎯 批阅状态分布</span>
            </div>
            <div ref="pieChartRef" style="height: 350px; width: 100%;"></div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { School, User, Document, Bell } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const router = useRouter()
const loading = ref(false)

const stats = ref({
  totalClasses: 0,
  totalStudents: 0,
  totalSubmissions: 0,
  pendingReview: 0,
  graded: 0,
  needRevise: 0,
  trendDates: [] as string[],
  trendCounts: [] as number[]
})

// 用于数字滚动的展示数据
const displayStats = ref({
  totalClasses: 0,
  totalStudents: 0,
  totalSubmissions: 0,
  pendingReview: 0
})

// 数字滚动动画
const animateNumber = (targetKey: keyof typeof displayStats.value, targetValue: number) => {
  const duration = 1000
  const startTime = performance.now()
  const startValue = 0

  const update = (currentTime: number) => {
    const elapsed = currentTime - startTime
    if (elapsed < duration) {
      const progress = elapsed / duration
      // easeOutQuart 缓动函数，让数字滚动更有质感
      const easeProgress = 1 - Math.pow(1 - progress, 4)
      displayStats.value[targetKey] = Math.floor(startValue + (targetValue - startValue) * easeProgress)
      requestAnimationFrame(update)
    } else {
      displayStats.value[targetKey] = targetValue
    }
  }
  requestAnimationFrame(update)
}

const lineChartRef = ref<HTMLElement | null>(null)
const pieChartRef = ref<HTMLElement | null>(null)
let lineChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/teacher/dashboard/stats')
    if (res.data.code === 200 || res.data.code === '200') {
      stats.value = res.data.data

      // 触发数字滚动
      animateNumber('totalClasses', stats.value.totalClasses)
      animateNumber('totalStudents', stats.value.totalStudents)
      animateNumber('totalSubmissions', stats.value.totalSubmissions)
      animateNumber('pendingReview', stats.value.pendingReview)

      await nextTick()
      initLineChart()
      initPieChart()
    }
  } catch (e) {
    ElMessage.error('获取看板数据失败')
  } finally {
    loading.value = false
  }
}

// 初始化折线图（带渐变面积）
const initLineChart = () => {
  if (!lineChartRef.value) return
  if (lineChart) lineChart.dispose()
  lineChart = echarts.init(lineChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#e8f5e9',
      textStyle: { color: '#333' },
      extraCssText: 'box-shadow: 0 4px 12px rgba(0,0,0,0.1); border-radius: 8px;'
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: stats.value.trendDates,
      axisLine: { lineStyle: { color: '#e0e0e0' } },
      axisLabel: { color: '#888' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#888' },
      splitLine: { lineStyle: { type: 'dashed', color: '#f0f0f0' } }
    },
    series: [
      {
        name: '提交数量',
        type: 'line',
        smooth: true, // 平滑曲线
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: { color: '#4caf50', borderColor: '#fff', borderWidth: 2 },
        lineStyle: { width: 4, color: '#4caf50' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(76, 175, 80, 0.4)' },
            { offset: 1, color: 'rgba(76, 175, 80, 0.01)' }
          ])
        },
        data: stats.value.trendCounts
      }
    ]
  }
  lineChart.setOption(option)
}

// 初始化玫瑰图
const initPieChart = () => {
  if (!pieChartRef.value || stats.value.totalSubmissions === 0) return
  if (pieChart) pieChart.dispose()
  pieChart = echarts.init(pieChartRef.value)

  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: '0%', icon: 'circle', itemGap: 20 },
    color: ['#e6a23c', '#67c23a', '#f56c6c'], // 黄，绿，红
    series: [
      {
        name: '作业状态',
        type: 'pie',
        radius: ['25%', '75%'], // 玫瑰图的半径范围
        center: ['50%', '45%'],
        roseType: 'area', // 玫瑰图
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        data: [
          { value: stats.value.pendingReview, name: '待批阅' },
          { value: stats.value.graded, name: '已通过' },
          { value: stats.value.needRevise, name: '需重做' }
        ]
      }
    ]
  }
  pieChart.setOption(option)
}

const handleResize = () => {
  lineChart?.resize()
  pieChart?.resize()
}

onMounted(() => {
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  lineChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
/* 现代 SaaS 背景 */
.dashboard-container {
  padding: 30px;
  background-color: #f7f9fc;
  min-height: calc(100vh - 60px);
  position: relative;
  overflow: hidden;
}

/* 背景光晕 */
.bg-glow {
  position: absolute;
  top: -100px;
  right: -50px;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(76, 175, 80, 0.15) 0%, rgba(255, 255, 255, 0) 70%);
  z-index: 0;
  pointer-events: none;
}

/* 欢迎横幅 */
.welcome-banner {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px); /* 玻璃拟态 */
  padding: 24px 30px;
  border-radius: 20px;
  box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.8);
  margin-bottom: 24px;
}
.welcome-text h2 { margin: 0 0 8px 0; color: #2e7d32; font-size: 24px; }
.welcome-text p { margin: 0; color: #666; font-size: 14px; }
.action-btn { background-color: #4caf50; border: none; padding: 12px 28px; font-weight: bold; }
.action-btn:hover { background-color: #43a047; transform: translateY(-2px); transition: all 0.3s; }

/* KPI 卡片 */
.kpi-row { margin-bottom: 24px; position: relative; z-index: 1; }
.kpi-card {
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 10px 20px -5px rgba(0, 0, 0, 0.03);
  transition: all 0.3s ease;
  border: 1px solid transparent;
}
.kpi-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 25px -5px rgba(0, 0, 0, 0.08);
  border-color: #e8f5e9;
}
.kpi-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}
.kpi-title { font-size: 15px; color: #666; font-weight: 500; }
.kpi-icon {
  width: 48px; height: 48px; border-radius: 14px;
  display: flex; justify-content: center; align-items: center;
  font-size: 24px;
}
.kpi-value { font-size: 36px; font-weight: 800; color: #1a1a1a; letter-spacing: -1px; }

/* 卡片主题色 */
.blue-card .kpi-icon { background: #e6f7ff; color: #409eff; }
.green-card .kpi-icon { background: #e8f5e9; color: #4caf50; }
.orange-card .kpi-icon { background: #fff7e6; color: #fa8c16; }
.red-card .kpi-icon { background: #fff1f0; color: #f5222d; }

/* 图表区 */
.chart-row { position: relative; z-index: 1; }
.chart-card {
  background: #fff;
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 10px 20px -5px rgba(0, 0, 0, 0.03);
  height: 100%;
}
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.chart-title { font-size: 16px; font-weight: bold; color: #333; }
</style>
