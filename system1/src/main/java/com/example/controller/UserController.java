package com.example.controller;

import com.example.common.Result;
import com.example.entity.SysUser;
import com.example.service.UserService;
import com.example.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    // 🔥 提取 Token 中 UserId 的辅助方法
    private Long getUserIdFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("未登录或 Token 格式错误");
        }
        Claims claims = jwtUtil.parseToken(token.substring(7));
        return Long.valueOf(claims.getSubject());
    }

    // 获取当前登录用户的个人信息
    @GetMapping("/info")
    public Result getUserInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = getUserIdFromToken(token);
            SysUser user = userService.getUserInfo(userId);
            return Result.success(user);
        } catch (Exception e) {
            return Result.error("获取信息失败：" + e.getMessage());
        }
    }

    // 更新个人信息
    @PostMapping("/update")
    public Result updateProfile(@RequestHeader("Authorization") String token, @RequestBody SysUser user) {
        try {
            Long userId = getUserIdFromToken(token);
            user.setId(userId); // 防止前端越权修改别人信息
            userService.updateProfile(user);
            return Result.success("更新成功");
        } catch (Exception e) {
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    // 修改密码
    @PostMapping("/password")
    public Result changePassword(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> request) {
        try {
            Long userId = getUserIdFromToken(token);
            String oldPwd = request.get("oldPassword");
            String newPwd = request.get("newPassword");
            userService.changePassword(userId, oldPwd, newPwd);
            return Result.success("密码修改成功，请重新登录");
        } catch (Exception e) {
            return Result.error("密码修改失败：" + e.getMessage());
        }
    }

    // 教师端：根据 classId 获取该班级的所有学生
    @GetMapping("/class/{classId}/students")
    public Result getClassStudents(@RequestHeader("Authorization") String token, @PathVariable Long classId) {
        try {
            // 鉴权：此处应检查当前用户是否为 TEACHER
            List<SysUser> students = userService.getStudentsByClassId(classId);
            return Result.success(students);
        } catch (Exception e) {
            return Result.error("查询学生失败：" + e.getMessage());
        }
    }
}