package com.example.dto;

import lombok.Data;
//用来接收前端传来的数据
@Data
public class LoginDTO {
    private String username;
    private String password;
    private String role; // 前端传过来的身份选择：STUDENT/TEACHER/SUB_ADMIN/MAIN_ADMIN
}