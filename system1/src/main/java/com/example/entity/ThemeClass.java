package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("theme_class")
public class ThemeClass {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long themeId;
    private Long classId;
    private LocalDateTime createdAt;
}