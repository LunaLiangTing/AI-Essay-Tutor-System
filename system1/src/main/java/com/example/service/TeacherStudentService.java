package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.entity.SysUser;
import com.example.entity.TClass;
import com.example.mapper.ClassMapper;
import com.example.mapper.UserMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 教师端管理学生的业务（主体是教师）
@Service
public class TeacherStudentService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ClassMapper classMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 1. 解析 Excel 并批量导入学生
    public List<Map<String, String>> importStudents(MultipartFile file, Long classId, Long teacherId) throws Exception {
        TClass tClass = classMapper.selectById(classId);
        if (tClass == null || !tClass.getTeacherId().equals(teacherId)) {
            throw new RuntimeException("无权限操作该班级");
        }

        List<SysUser> newStudents = new ArrayList<>();
        List<Map<String, String>> resultList = new ArrayList<>(); // 用于返回给前端的明文列表

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getCellValue(row.getCell(0));
                String gender = getCellValue(row.getCell(1));
                String studentNo = getCellValue(row.getCell(2));

                if (name.isEmpty() || studentNo.isEmpty()) continue;

                // 生成用户名和密码
                String pinyinInitials = cn.hutool.extra.pinyin.PinyinUtil.getFirstLetter(name, "").toLowerCase();
                String username = pinyinInitials;
                while (userMapper.selectCount(new QueryWrapper<SysUser>().eq("username", username)) > 0) {
                    username = username + "1";
                }

                String rawPassword = studentNo + username; // 例如：20230001zs
                String encodedPassword = passwordEncoder.encode(rawPassword);

                // 🔥 修复：先 new 对象，再 set 属性
                SysUser student = new SysUser();
                student.setName(name);
                student.setGender(gender);
                student.setStudentId(studentNo);
                student.setClassId(classId);
                student.setClassNum(tClass.getClassCode());
                student.setRole("STUDENT");
                student.setUsername(username);
                student.setPassword(encodedPassword);
                student.setInitPassword(rawPassword); // 🔥 存入明文供展示
                student.setStatus(0);
                student.setCreatedAt(LocalDateTime.now());
                student.setUpdatedAt(LocalDateTime.now());

                newStudents.add(student);

                // 收集明文账号密码用于返回
                Map<String, String> accountInfo = new HashMap<>();
                accountInfo.put("name", name);
                accountInfo.put("username", username);
                accountInfo.put("password", rawPassword);
                resultList.add(accountInfo);
            }
        }

        // 保存到数据库
        if (!newStudents.isEmpty()) {
            for (SysUser s : newStudents) {
                if (userMapper.selectCount(new QueryWrapper<SysUser>().eq("username", s.getUsername())) == 0) {
                    userMapper.insert(s);
                }
            }
            Long totalCount = userMapper.selectCount(new QueryWrapper<SysUser>().eq("class_id", classId));
            tClass.setStudentCount(totalCount.intValue());
            classMapper.updateById(tClass);
        }

        return resultList;
    }

    // 2. 获取班级学生列表
    public List<SysUser> getStudentsByClass(Long classId) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("class_id", classId).eq("role", "STUDENT");
        return userMapper.selectList(wrapper);
    }

    // 3. 移除学生（软移除）
    public void deleteStudent(Long studentId, Long classId) {
        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysUser::getId, studentId)
                .eq(SysUser::getClassId, classId)
                .set(SysUser::getClassId, null)
                .set(SysUser::getClassNum, null)
                .set(SysUser::getStatus, 0)
                .set(SysUser::getInitPassword, null); // 🔥 移除时清空明文密码

        userMapper.update(null, updateWrapper);

        TClass tClass = classMapper.selectById(classId);
        if (tClass != null) {
            Long totalCount = userMapper.selectCount(new QueryWrapper<SysUser>().eq("class_id", classId));
            tClass.setStudentCount(totalCount.intValue());
            classMapper.updateById(tClass);
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
}