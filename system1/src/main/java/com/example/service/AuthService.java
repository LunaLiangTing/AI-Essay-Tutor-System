package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.dto.LoginDTO;
import com.example.dto.RegisterDTO;
import com.example.entity.AdminInvite;
import com.example.entity.SysUser;
import com.example.mapper.AdminInviteMapper;
import com.example.mapper.UserMapper;
import com.example.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
//AuthService 是用来处理登录、注册业务逻辑的。这段代码已经包含了登录注册的所有功能（登录、注册、密码加密、子管理员邀请码校验）。
@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AdminInviteMapper adminInviteMapper;

    @Autowired
    private JwtUtil jwtUtil;

    // 密码加密器（注意：这是 Spring Security 提供的，不需要引入完整的安全框架）
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ============== 登录逻辑 ==============
    public Map<String, Object> login(LoginDTO dto) {
        // 1. 根据用户名查询用户
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", dto.getUsername()));

        // 2. 检查用户是否存在
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 校验密码（BCrypt 自动比对加密后的字符串）
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 4. 校验身份是否匹配（防止学生选了教师的身份登录）
        if (!user.getRole().equals(dto.getRole())) {
            throw new RuntimeException("身份选择错误，请重新选择");
        }
        // 5. 校验账号状态
        if (user.getStatus() == 0) {
            // 🔥 核心修改：如果是学生且未激活，允许登录（让他去前端弹窗验证班级码）
            // 如果是老师或管理员被禁用，则直接拦截
            if (!"STUDENT".equals(user.getRole())) {
                throw new RuntimeException("账号未激活或已被禁用");
            }
        }
        // 6. 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        // 7. 返回前端需要的数据
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("name", user.getName());
        // ... 前面校验密码等代码不变 ...
        result.put("status", user.getStatus()); // 🔥 新增：返回用户状态
        return result;
    }

    // ============== 注册逻辑 ==============
    public void register(RegisterDTO dto) {
        // 1. 检查用户名是否重复
        SysUser existUser = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", dto.getUsername()));
        if (existUser != null) {
            throw new RuntimeException("该用户名已被注册，请换一个");
        }

        SysUser newUser = new SysUser();
        newUser.setUsername(dto.getUsername());
        // 密码加密存库
        newUser.setPassword(passwordEncoder.encode(dto.getPassword()));
        newUser.setRole(dto.getRole());
        newUser.setName(dto.getName() != null ? dto.getName() : dto.getUsername());
        newUser.setStatus(1);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        // 2. 判断角色特殊逻辑
        if ("SUB_ADMIN".equals(dto.getRole())) {
            // 如果是子管理员，必须校验邀请码
            if (dto.getInviteCode() == null || dto.getInviteCode().trim().isEmpty()) {
                throw new RuntimeException("子管理员注册必须提供有效邀请码");
            }

            // 查库校验邀请码
            AdminInvite invite = adminInviteMapper.selectOne(new QueryWrapper<AdminInvite>().eq("invite_code", dto.getInviteCode()));

            if (invite == null) {
                throw new RuntimeException("无效的邀请码，请向主管理员核对");
            }
            if (invite.getIsUsed()) {
                throw new RuntimeException("该邀请码已被使用，请向主管理员索要新的");
            }

            // 标记为已使用
            invite.setIsUsed(true);
            adminInviteMapper.updateById(invite);

        } else if ("MAIN_ADMIN".equals(dto.getRole())) {
            // 主管理员不允许直接注册
            throw new RuntimeException("主管理员账号无法直接注册");
        }

        // 3. 插入数据库
        userMapper.insert(newUser);
    }

    public void verifyClassCode(Long userId, String inputClassCode) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");

        // 校验班级码是否匹配（数据库里存的 class_num 是老师导入时绑定的那个码）
        if (!inputClassCode.equals(user.getClassNum())) {
            throw new RuntimeException("班级码错误，请核对老师发给你的号码");
        }

        // 验证通过，把状态改成 1（正常）
        user.setStatus(1);
        userMapper.updateById(user);
    }
}