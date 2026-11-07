package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("essay_submission")
public class EssaySubmission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long taskId;

    private Long sessionId;

    private String title;

    private String opening;

    private String middle;

    private String ending;

    private String reflection;

    private String status;

    private Integer score;

    private String teacherComment;

    private String revisedEssay;

    private String revisionReflection;

    private LocalDateTime submitTime;

    private LocalDateTime gradeTime;

    private LocalDateTime revisionTime;

    // 🔥 告诉 MyBatis-Plus 数据库里没这个字段，查库时忽略它
    @TableField(exist = false)
    private Integer revisionCount;
    // ==========================================
    // 🔥 新增：用于前端展示的扩展字段public
    // 加上 @TableField(exist = false) 告诉 MyBatis-Plus
    // 这几个字段在数据库表里不存在，不要在查库/插入时带上它们
    // ==========================================

    @TableField(exist = false)
    private String studentName; // 学生真实姓名

    @TableField(exist = false)
    private String studentNo; // 学生学号

    @TableField(exist = false)
    private String taskTitle; // 任务标题
}