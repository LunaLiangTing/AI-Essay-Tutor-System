package com.example.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String role; // 学生、教师、子管理员
    private String name; // 真实姓名
    private String inviteCode; // 如果是子管理员注册，必须传这个邀请码
}