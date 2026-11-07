package com.example.controller;

import com.example.common.Result;
import com.example.dto.LoginDTO;
import com.example.dto.RegisterDTO;
import com.example.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户登录接口
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO loginDTO) {
        try {
            // 调用 Service 层获取 Token 和用户信息
            Map<String, Object> data = authService.login(loginDTO);
            return Result.success(data);
        } catch (Exception e) {
            // 如果抛出异常（如密码错误），返回错误信息给前端
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户注册接口
     */
    @PostMapping("/register")
    public Result register(@RequestBody RegisterDTO registerDTO) {
        try {
            // 调用 Service 层执行注册逻辑（密码加密、邀请码校验等）
            authService.register(registerDTO);
            return Result.success("注册成功，请登录");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/verify-class")
    public Result verifyClass(@RequestBody Map<String, String> request, @RequestAttribute("userId") Long userId) {
        try {
            String classCode = request.get("classCode");
            authService.verifyClassCode(userId, classCode);
            return Result.success("班级验证成功，欢迎加入！");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}