package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("essay_theme")
public class EssayTheme {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String themeName;
    private String teachingObjective;
    private String teachingGoal;
    private Long teacherId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}