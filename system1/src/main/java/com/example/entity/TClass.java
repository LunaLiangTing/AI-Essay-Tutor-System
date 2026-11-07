package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_class")
public class TClass {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String className;
    private String classCode; // 5位唯一码
    private Long teacherId; // 班主任ID
    private Integer studentCount;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}