package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dto.PublishThemeDTO;
import com.example.dto.TaskDTO;
import com.example.dto.ThemeDTO;
import com.example.entity.EssayTask;
import com.example.entity.EssayTheme;
import com.example.entity.ThemeClass;
import com.example.mapper.EssayTaskMapper;
import com.example.mapper.EssayThemeMapper;
import com.example.mapper.ThemeClassMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TeacherTaskService {

    @Autowired
    private EssayThemeMapper themeMapper;
    @Autowired
    private EssayTaskMapper taskMapper;
    @Autowired
    private ThemeClassMapper themeClassMapper;

    // ========== 主题管理 ==========
    public Page<EssayTheme> getThemes(Long teacherId, Integer pageNum, Integer pageSize) {
        Page<EssayTheme> page = new Page<>(pageNum, pageSize);
        QueryWrapper<EssayTheme> wrapper = new QueryWrapper<>();
        wrapper.eq("teacher_id", teacherId).orderByDesc("created_at");
        return themeMapper.selectPage(page, wrapper);
    }

    public void createTheme(ThemeDTO dto, Long teacherId) {
        EssayTheme theme = new EssayTheme();
        theme.setThemeName(dto.getThemeName());
        theme.setTeachingObjective(dto.getTeachingObjective());
        theme.setTeachingGoal(dto.getTeachingGoal());
        theme.setTeacherId(teacherId);
        theme.setCreatedAt(LocalDateTime.now());
        theme.setUpdatedAt(LocalDateTime.now());
        themeMapper.insert(theme);
    }

    // 🔥 新增：更新主题
    public void updateTheme(Long themeId, ThemeDTO dto, Long teacherId) {
        EssayTheme theme = themeMapper.selectById(themeId);
        if (theme == null) {
            throw new RuntimeException("主题不存在");
        }
        // 安全校验：只能修改自己创建的主题
        if (!theme.getTeacherId().equals(teacherId)) {
            throw new RuntimeException("无权限修改此主题");
        }

        theme.setThemeName(dto.getThemeName());
        theme.setTeachingObjective(dto.getTeachingObjective());
        theme.setTeachingGoal(dto.getTeachingGoal());
        theme.setUpdatedAt(LocalDateTime.now());

        themeMapper.updateById(theme);
    }

    // 获取某个主题已经发布的班级ID列表（用于前端回显）
    public List<Long> getPublishedClassIds(Long themeId) {
        return themeClassMapper.selectList(
                new QueryWrapper<ThemeClass>().eq("theme_id", themeId)
        ).stream().map(ThemeClass::getClassId).collect(java.util.stream.Collectors.toList());
    }

    // 保存发布关系
    public void publishThemeToClasses(PublishThemeDTO dto) {
        // 1. 先删除该主题之前的发布记录（覆盖式更新）
        themeClassMapper.delete(new QueryWrapper<ThemeClass>().eq("theme_id", dto.getThemeId()));

        // 2. 再插入新的记录
        if (dto.getClassIds() != null && !dto.getClassIds().isEmpty()) {
            for (Long classId : dto.getClassIds()) {
                ThemeClass tc = new ThemeClass();
                tc.setThemeId(dto.getThemeId());
                tc.setClassId(classId);
                tc.setCreatedAt(LocalDateTime.now());
                themeClassMapper.insert(tc);
            }
        }
    }

    // ========== 任务管理 ==========
    public List<EssayTask> getTasksByTheme(Long themeId) {
        return taskMapper.selectList(new QueryWrapper<EssayTask>()
                .eq("theme_id", themeId)
                .orderByDesc("created_at"));
    }

    public void createTask(TaskDTO dto) {
        EssayTask task = new EssayTask();
        task.setThemeId(dto.getThemeId());
        task.setTitle(dto.getTitle());
        task.setImageUrl(dto.getImageUrl());
        task.setSystemPrompt(dto.getSystemPrompt());
        task.setIsUpdated(false);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
    }

    public void deleteTask(Long taskId) {
        taskMapper.deleteById(taskId);
    }

    // 🔥 新增：根据ID获取单个任务
    public EssayTask getTaskById(Long taskId) {
        EssayTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        return task;
    }

    // 🔥 新增：更新任务
    public void updateTask(EssayTask task) {
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }
}