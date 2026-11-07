package com.example.dto;

import java.util.List;

//使用的是 RestTemplate（底层是 HTTP 协议），发送请求时，Spring Boot 默认会把 Java 对象自动转换成 JSON 格式的字符串发给 DeepSeek。如果没有这个类，你就得手动去拼接丑陋且容易出错的 JSON 字符串了
//DeepSeek 的 API 兼容 OpenAI 的格式，它的请求体（Request Body）必须包含模型名字（model）和对话消息（messages）。
// 这是外层请求体
public record DeepSeekRequest(
        String model,               // 模型名称，如 "deepseek-chat"
        List<Message> messages,     // 对话消息列表
        double temperature,         // 随机性（0-2之间，越大约随机）
        int max_tokens              // 最大生成的 token 数量
) {
    // 这是内部类，代表单条消息（包含角色和内容）
    public record Message(
            String role,            // "system" 或 "user" 或 "assistant"
            String content          // 具体的提示词内容
    ) {}
}