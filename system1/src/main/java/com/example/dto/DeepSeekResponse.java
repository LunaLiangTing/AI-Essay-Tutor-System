package com.example.dto;

import java.util.List;
//既然你发请求（Request）需要一个类来封装，那么接收 DeepSeek 返回的结果（Response）也需要一个类来接收。

public record DeepSeekResponse(
        List<Choice> choices
) {
    public record Choice(
            Message message
    ) {}

    public record Message(
            String role,
            String content
    ) {}
}
//这样一来，在DeepSeekService中，通过 response.getBody().choices().get(0).message().content() 就能直接拿到 AI 回答的文字了，完全不需要手动去解析 JSON！