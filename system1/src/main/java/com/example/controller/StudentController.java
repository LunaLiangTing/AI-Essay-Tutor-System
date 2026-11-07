package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.Result;
import com.example.entity.ChatMessage;
import com.example.entity.ChatSession;
import com.example.entity.EssaySubmission;
import com.example.entity.EssayTask;
import com.example.mapper.ChatMessageMapper;
import com.example.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentService studentService;
    //因为后面要获取历史聊天记录，获取单篇作文详情（用于回显）
    @Autowired
    private ChatMessageMapper chatMessageMapper; // 🔥 确保注入了这个
    /**
     * 1. 获取我的任务列表 (对应学生端主页)
     */
    @GetMapping("/tasks")
    public Result getMyTasks(@RequestAttribute("userId") Long studentId) {
        try {
            // 返回结构变成了 List<Map>
            return Result.success(studentService.getMyTasks(studentId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 2. 获取任务详情 (Study.vue 页面初始化时加载图片和提示词)
     */
    @GetMapping("/task/{taskId}")
    public Result getTaskDetail(@PathVariable Long taskId) {
        try {
            EssayTask task = studentService.getTaskDetail(taskId);
            return Result.success(task);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 3. 开启会话 (进入 Study.vue 时调用，生成或复用进行中的 sessionId)
     */
    @PostMapping("/session/start")
    public Result startSession(@RequestAttribute("userId") Long studentId, @RequestParam Long taskId) {
        try {
            ChatSession session = studentService.startSession(studentId, taskId);
            return Result.success(session);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 4. 保存一条对话记录 (每次学生或AI说话时调用，用于过程性评价)
     */
    @PostMapping("/message/save")
    public Result saveMessage(@RequestParam Long sessionId,
                              @RequestParam String role,
                              @RequestParam String content) {
        try {
            studentService.saveMessage(sessionId, role, content);
            return Result.success("保存成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 5. 提交最终作文 (对应 Study.vue 左下角的提交按钮)
     */
    @PostMapping("/essay/submit")
    public Result submitEssay(@RequestAttribute("userId") Long studentId,
                              @RequestBody EssaySubmission submission) {
        try {
            // 强制把当前登录学生的ID赋给提交记录，防止越权
            submission.setStudentId(studentId);
            studentService.submitEssay(submission);
            return Result.success("作文提交成功！");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 6. 获取我的历史作文列表
     */
    @GetMapping("/essays")
    public Result getMyEssays(@RequestAttribute("userId") Long studentId) {
        try {
            List<EssaySubmission> essays = studentService.getMyEssays(studentId);
            return Result.success(essays);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 7. 获取单篇作文详情 (用于查看老师评语和二次修改)
     */
    @GetMapping("/essay/{submissionId}")
    public Result getEssayDetail(@PathVariable Long submissionId) {
        try {
            EssaySubmission essay = studentService.getEssayDetail(submissionId);
            return Result.success(essay);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 8. 提交二次修改的作文
     */
    @PostMapping("/essay/revise")
    public Result reviseEssay(@RequestParam Long submissionId,
                              @RequestParam String revisedEssay,
                              @RequestParam String revisionReflection) {
        try {
            studentService.reviseEssay(submissionId, revisedEssay, revisionReflection);
            return Result.success("二次修改提交成功！");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    // 🔥 新增：获取某个会话的历史聊天记录（用于重新进入 Study.vue 时回显），使用场景是第一次的作文被老师打回之后，如果学生还想继续和AI对话完成作文则可以回显到Study.vue这个页面
    @GetMapping("/session/messages/{sessionId}")
    public Result getSessionMessages(@PathVariable Long sessionId) {
        try {
            List<ChatMessage> messages = chatMessageMapper.selectList(
                    new QueryWrapper<ChatMessage>().eq("session_id", sessionId).orderByAsc("created_at")
            );
            // 只把 role 和 content 发给前端
            List<Map<String, String>> history = new ArrayList<>();
            for (ChatMessage msg : messages) {
                history.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
            }
            return Result.success(history);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

}