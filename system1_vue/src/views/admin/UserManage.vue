<template>
  <div class="page-container">
    <el-card class="green-card">
      <template #header>
        <div class="card-header">
          <h3>👥 用户管理</h3>
        </div>
      </template>
      <el-empty description="这里将查看所有教师和学生用户信息，页面开发中..." />
    </el-card>
  </div>
</template>

<style scoped>
.page-container { padding: 10px; }
.green-card { border-radius: 12px; border: 1px solid #dcedc8; }
.card-header h3 { margin: 0; color: #2e7d32; }
</style>
