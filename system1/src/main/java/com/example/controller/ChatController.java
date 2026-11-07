package com.example.controller;

import com.example.common.Result;
import com.example.dto.DeepSeekRequest; // 🔥 引入 Message 所在的类
import com.example.entity.ChatMessage;
import com.example.entity.EssayTask;
import com.example.mapper.ChatMessageMapper;
import com.example.mapper.EssayTaskMapper;
import com.example.service.DeepSeekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private EssayTaskMapper essayTaskMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper; // 🔥 注入消息表 Mapper

    @PostMapping("/chat")
    public Result chat(@RequestBody Map<String, Object> request) {
        try {
            // 1. 获取前端传来的 taskId 和 历史记录
            Long taskId = Long.valueOf(request.get("taskId").toString());
            Long sessionId = Long.valueOf(request.get("sessionId").toString()); // 🔥 获取会话ID
            String userMessage = request.get("userMessage").toString(); // 🔥 获取学生当前输入
            List<Map<String, String>> historyMap = (List<Map<String, String>>) request.get("history");
            // 1. 把学生说的话存入数据库
            saveMessage(sessionId, "user", userMessage);
            // 2. 去数据库查这个任务的最新提示词
            EssayTask task = essayTaskMapper.selectById(taskId);
            String systemPrompt = task.getSystemPrompt();
            if (task == null) {
                return Result.error("找不到对应的任务信息");
            }

            // 3.将 List<Map> 转换为 List<DeepSeekRequest.Message>
            List<DeepSeekRequest.Message> historyMessages = new ArrayList<>();
            if (historyMap != null) {
                for (Map<String, String> msgMap : historyMap) {
                    historyMessages.add(new DeepSeekRequest.Message(
                            msgMap.get("role"),
                            msgMap.get("content")
                    ));
                }
            }
            // 必须把前端刚传过来的 userMessage（表示当前学生说了什么内容) 拼接到历史的最后面！如果不加下面这行代码，AI会一直拿不到当前用户说的这句话！
            historyMessages.add(new DeepSeekRequest.Message("user", userMessage));
            // 4. 调用 AI 服务，传入转换后的类型
            String reply = deepSeekService.getChatResponse(systemPrompt, historyMessages);
            // 3. 把 AI 说的话存入数据库
            saveMessage(sessionId, "assistant", reply);
            return Result.success(reply);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("AI 对话失败：" + e.getMessage());
        }
    }
    // 辅助方法：保存消息
    private void saveMessage(Long sessionId, String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(msg);
    }

}