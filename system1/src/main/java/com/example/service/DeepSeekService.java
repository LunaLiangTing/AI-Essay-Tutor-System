package com.example.service;

import com.example.dto.DeepSeekRequest;
import com.example.dto.DeepSeekResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeepSeekService {
    @Value("${deepseek.api-key}")
    private String apiKey;
    @Value("${deepseek.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    // 这里直接注入 RestTemplate，Spring 会自动把我们刚才 config 包里的那个 Bean 传进来
    public DeepSeekService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    /**
     * 改造方法：接收动态的 System Prompt 和 历史记录列表
     * @param systemPrompt 老师为当前任务配置的专属提示词
     * @param history 前端传过来的对话历史
     * @return AI 的回复内容
     */
    public String getChatResponse(String systemPrompt, List<DeepSeekRequest.Message> history) {

        // 组装最终发给 DeepSeek 的消息列表
        List<DeepSeekRequest.Message> finalMessages = new ArrayList<>();

        // 1. 第一条永远是 System Prompt，告诉 AI 它的角色和规则
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            finalMessages.add(new DeepSeekRequest.Message("system", systemPrompt));
        } else {
            // 兜底方案：防止极端情况下老师漏传，AI 变成无头苍蝇
            finalMessages.add(new DeepSeekRequest.Message("system", "你是一个小学作文辅导老师，请引导学生一步步完成作文。"));
        }

        // 2. 把前端的对话历史加上
        finalMessages.addAll(history);

        DeepSeekRequest request = new DeepSeekRequest(
                "deepseek-chat",
                finalMessages,
                0.7,
                2048 // 写作文建议把 max_tokens 调大一点，防止 AI 话没说完就被截断
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<DeepSeekRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<DeepSeekResponse> response = restTemplate.postForEntity(
                apiUrl, entity, DeepSeekResponse.class
        );

        return response.getBody().choices().get(0).message().content();
    }

}