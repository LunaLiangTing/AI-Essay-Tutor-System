package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user") // 🔥 新增
public class SysUser {
    @TableId(type = IdType.AUTO) // 🔥 表示 ID 交给数据库去自增分配，MP 不要去干涉它。
    private Long id;
    private String name;
    private Long schoolId;
    private Long classId;
    private String classNum;
    private String gender;
    private String role;
    private String studentId;
    private String username;
    private String password;
    private String avatar;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // 🔥 新增：初始明文密码
    private String initPassword;
}