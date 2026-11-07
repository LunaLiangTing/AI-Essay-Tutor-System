# 🎓 基于大语言模型的智能作文导学系统 (AI-Essay Tutor System)

## 📖 项目简介
本项目是一个面向基础教育的智能导学系统（Intelligent Tutoring System, ITS），结合了生成式人工智能（DeepSeek）与多模态交互技术。系统严格遵循教育心理学原理，旨在为学生提供即时的“支架式教学”，并为教师提供完整的“过程性评价”与“学情数据看板”。

## ✨ 核心教育技术亮点
*   **【支架式教学 (Scaffolding)】**：AI不直接代写，而是通过动态 System Prompt（如五感法、移步换景）一步步引导学生跨越“最近发展区(ZPD)”。
*   **【掌握学习 (Mastery Learning)】**：教师可设置及格阈值，未达标的作文将被自动“打回重做”，直至学生通过AI辅导再次提交。
*   **【过程性评价 (Formative Assessment)】**：系统留存全部对话记录与修改轨迹（Timeline），帮助教师分析学生的认知进化路径。
*   **【多模态交互 (Multimodal)】**：集成腾讯云 ASR/TTS，实现极低延迟的语音输入与实时流式播报（WebSocket + PCM），降低低年级学生的打字认知负荷。

## 🛠️ 技术栈
*   **前端**：Vue 3 + TypeScript + Vite + Element Plus + ECharts (数据可视化)
*   **后端**：Java 17 + Spring Boot 3 + MyBatis-Plus
*   **数据库**：PostgreSQL
*   **鉴权**：Spring Security + JWT (RBAC 多角色隔离)
*   **AI 引擎**：DeepSeek API、腾讯云流式 ASR/TTS

## 🚀 快速启动
### 后端 (Backend)
1. 导入 PostgreSQL 数据库。
2. 配置环境变量：`DEEPSEEK_API_KEY`, `TENCENT_SECRET_ID` 等。
3. 运行 `System1Application.java`。

### 前端 (Frontend)
```bash
cd system1_vue
npm install
npm run dev

