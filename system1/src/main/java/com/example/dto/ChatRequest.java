package com.example.dto;

import java.util.List;

// 加上 public 和 record 关键字
public record ChatRequest(
        List<DeepSeekRequest.Message> history
) {}