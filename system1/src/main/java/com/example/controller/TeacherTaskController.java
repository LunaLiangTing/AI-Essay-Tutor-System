package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.Result;
import com.example.dto.PublishThemeDTO;
import com.example.dto.TaskDTO;
import com.example.dto.ThemeDTO;
import com.example.entity.EssayTask;
import com.example.entity.EssayTheme;
import com.example.service.TeacherTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/task")
public class TeacherTaskController {

    @Autowired
    private TeacherTaskService teacherTaskService;

    // 1. 获取主题列表（分页）
    @GetMapping("/themes")
    public Result getThemes(@RequestAttribute("userId") Long teacherId,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<EssayTheme> page = teacherTaskService.getThemes(teacherId, pageNum, pageSize);
        return Result.success(page);
    }

    // 2. 新建主题
    @PostMapping("/theme/create")
    public Result createTheme(@RequestAttribute("userId") Long teacherId, @RequestBody ThemeDTO dto) {
        teacherTaskService.createTheme(dto, teacherId);
        return Result.success("主题创建成功");
    }

    // 3. 获取某个主题下的所有任务
    @GetMapping("/list/{themeId}")
    public Result getTasks(@PathVariable Long themeId) {
        List<EssayTask> list = teacherTaskService.getTasksByTheme(themeId);
        return Result.success(list);
    }

    // 🔥 新增：修改主题
    @PostMapping("/theme/update/{themeId}")
    public Result updateTheme(@RequestAttribute("userId") Long teacherId,
                              @PathVariable Long themeId,
                              @RequestBody ThemeDTO dto) {
        try {
            teacherTaskService.updateTheme(themeId, dto, teacherId);
            return Result.success("主题更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 4. 新建任务
    @PostMapping("/create")
    public Result createTask(@RequestBody TaskDTO dto) {
        teacherTaskService.createTask(dto);
        return Result.success("任务发布成功");
    }

    // 获取主题已发布的班级
    @GetMapping("/theme/{themeId}/classes")
    public Result getPublishedClasses(@PathVariable Long themeId) {
        return Result.success(teacherTaskService.getPublishedClassIds(themeId));
    }

    // 发布主题给勾选的班级
    @PostMapping("/theme/publish")
    public Result publishTheme(@RequestBody PublishThemeDTO dto) {
        try {
            teacherTaskService.publishThemeToClasses(dto);
            return Result.success("主题发布成功");
        } catch (Exception e) {
            return Result.error("发布失败：" + e.getMessage());
        }
    }
    // 5. 删除任务
    @DeleteMapping("/{taskId}")
    public Result deleteTask(@PathVariable Long taskId) {
        teacherTaskService.deleteTask(taskId);
        return Result.success("任务已删除");
    }

    // 🔥 5. 修改任务
    @PostMapping("/update/{taskId}")
    public Result updateTask(@PathVariable Long taskId, @RequestBody TaskDTO dto) {
        try {
            EssayTask task = teacherTaskService.getTaskById(taskId);
            task.setTitle(dto.getTitle());
            task.setImageUrl(dto.getImageUrl());
            task.setSystemPrompt(dto.getSystemPrompt());
            task.setIsUpdated(true); // 标记为已修改
            teacherTaskService.updateTask(task);
            return Result.success("任务更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}