package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dto.CreateClassDTO;
import com.example.entity.TClass;
import com.example.mapper.ClassMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class ClassService {

    @Autowired
    private ClassMapper classMapper;

    // 1. 创建班级
    public String createClass(CreateClassDTO dto, Long teacherId) {
        TClass tClass = new TClass();
        tClass.setClassName(dto.getClassName());
        tClass.setStudentCount(dto.getStudentCount() != null ? dto.getStudentCount() : 0);
        tClass.setRemark(dto.getRemark());
        tClass.setTeacherId(teacherId);

        // 生成 5 位唯一班级编号
        tClass.setClassCode(generateUniqueClassCode());

        tClass.setCreatedAt(LocalDateTime.now());
        tClass.setUpdatedAt(LocalDateTime.now());

        classMapper.insert(tClass);
        return tClass.getClassCode();
    }

    // 2. 获取当前教师的所有班级（物理分页版）
    public Page<TClass> getTeacherClasses(Long teacherId, Integer pageNum, Integer pageSize) {
        // 构建分页对象
        Page<TClass> page = new Page<>(pageNum, pageSize);
        QueryWrapper<TClass> wrapper = new QueryWrapper<>();
        wrapper.eq("teacher_id", teacherId).orderByDesc("created_at");

        // 调用 MP 的 selectPage 方法进行物理分页
        return classMapper.selectPage(page, wrapper);
    }

    // 🔥 3. 新增：更新班级信息
    public void updateClass(Long classId, CreateClassDTO dto, Long teacherId) {
        // 先查询班级是否存在
        TClass tClass = classMapper.selectById(classId);
        if (tClass == null) {
            throw new RuntimeException("班级不存在");
        }

        // 安全校验：只能修改自己创建的班级
        if (!tClass.getTeacherId().equals(teacherId)) {
            throw new RuntimeException("无权限修改该班级");
        }

        // 更新允许修改的字段
        tClass.setClassName(dto.getClassName());
        tClass.setStudentCount(dto.getStudentCount());
        tClass.setRemark(dto.getRemark());
        tClass.setUpdatedAt(LocalDateTime.now());

        classMapper.updateById(tClass);
    }

    // 4. 删除班级
    public void deleteClass(Long classId, Long teacherId) {
        TClass tClass = classMapper.selectById(classId);
        if (tClass == null) throw new RuntimeException("班级不存在");
        // 安全校验：只能删除自己创建的班级
        if (!tClass.getTeacherId().equals(teacherId)) {
            throw new RuntimeException("无权限删除该班级");
        }
        classMapper.deleteById(classId);
    }

    // 5. 生成不重复的 5 位班级码
    private String generateUniqueClassCode() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random rnd = new Random();
        while (true) {
            StringBuilder sb = new StringBuilder(5);
            for (int i = 0; i < 5; i++) {
                sb.append(chars.charAt(rnd.nextInt(chars.length())));
            }
            String code = sb.toString();
            // 检查数据库中是否已存在该 code
            if (classMapper.selectCount(new QueryWrapper<TClass>().eq("class_code", code)) == 0) {
                return code;
            }
        }
    }
}