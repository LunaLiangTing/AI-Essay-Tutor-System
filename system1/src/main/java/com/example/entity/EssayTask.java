package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("essay_task")
public class EssayTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long themeId;
    private String title; // 例如：古镇老街
    private String imageUrl;
    private String systemPrompt; // 专属提示词
    private Boolean isUpdated; // 是否被老师修改过
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}