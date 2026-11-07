package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.Result;
import com.example.dto.CreateClassDTO;
import com.example.dto.UpdateClassDTO;
import com.example.entity.TClass;
import com.example.service.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/class")
public class ClassController {

    @Autowired
    private ClassService classService;

    // 1. 创建班级
    @PostMapping("/create")
    public Result createClass(@RequestAttribute("userId") Long teacherId,
                              @RequestBody CreateClassDTO dto) {
        try {
            String classCode = classService.createClass(dto, teacherId);
            return Result.success("创建成功，班级码：" + classCode);
        } catch (Exception e) {
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    // 2. 获取我的班级列表
    // 🔥 修改点：接收分页参数，默认第1页，每页10条
    @GetMapping("/list")
    public Result getMyClasses(
            @RequestAttribute("userId") Long teacherId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            Page<TClass> pageData = classService.getTeacherClasses(teacherId, pageNum, pageSize);
            return Result.success(pageData);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 3. 删除班级
    @DeleteMapping("/{classId}")
    public Result deleteClass(@RequestAttribute("userId") Long teacherId,
                              @PathVariable Long classId) {
        try {
            classService.deleteClass(classId, teacherId);
            return Result.success("班级已删除");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 🔥 缺失的更新接口，必须补上
    @PostMapping("/update/{classId}")
    public Result updateClass(@RequestAttribute("userId") Long teacherId,
                              @PathVariable Long classId,
                              @RequestBody CreateClassDTO dto) {
        try {
            classService.updateClass(classId, dto, teacherId);
            return Result.success("班级信息更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
