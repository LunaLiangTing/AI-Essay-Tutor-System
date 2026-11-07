package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.entity.SysUser;
import com.example.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 获取用户信息（去除密码字段，保证安全）
    public SysUser getUserInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null); // 不把密码传给前端
        }
        return user;
    }

    // 更新用户个人资料（只能改姓名、性别、头像等，不能改角色和密码）
    public void updateProfile(SysUser user) {
        SysUser existUser = userMapper.selectById(user.getId());
        if (existUser == null) throw new RuntimeException("用户不存在");

        existUser.setName(user.getName());
        existUser.setGender(user.getGender());
        existUser.setAvatar(user.getAvatar());
        existUser.setUpdatedAt(java.time.LocalDateTime.now());
        userMapper.updateById(existUser);
    }

    // 修改密码
    public void changePassword(Long userId, String oldPwd, String newPwd) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");

        // 校验旧密码
        if (!passwordEncoder.matches(oldPwd, user.getPassword())) {
            throw new RuntimeException("原密码错误，请重试");
        }

        // 更新新密码
        user.setPassword(passwordEncoder.encode(newPwd));
        userMapper.updateById(user);
    }

    // 教师端：查询某个班级的所有学生
    public List<SysUser> getStudentsByClassId(Long classId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getClassId, classId)
                .eq(SysUser::getRole, "STUDENT");
        return userMapper.selectList(wrapper);
    }
}