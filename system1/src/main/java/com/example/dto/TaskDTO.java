package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskDTO {
    private Long themeId;

    @NotBlank(message = "任务标题不能为空")
    private String title;

    // 🔥 修改点：去掉 @NotBlank，允许图片 URL 为空
    private String imageUrl;

    @NotBlank(message = "AI 专属提示词绝对不能为空！请认真填写辅导规则。")
    private String systemPrompt;
}