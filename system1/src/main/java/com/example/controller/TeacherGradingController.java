package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.Result;
import com.example.entity.*;
import com.example.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher/grading")
public class TeacherGradingController {

    @Autowired
    private EssaySubmissionMapper submissionMapper;
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private EssayTaskMapper taskMapper;
    //用于与学生提交的所有历史版本的文章的那张数据表进行关联
    @Autowired
    private EssayRevisionHistoryMapper historyMapper;

    // 1. 获取某班级的所有提交记录（关联学生姓名、学号、任务标题）
    @GetMapping("/list")
    public Result getSubmissions(@RequestParam(required = false) Long classId) {
        // 🔥 修复：如果没传 classId，直接返回空列表
        if (classId == null) {
            return Result.success(new ArrayList<>());
        }

        // 第一步：查出这个班级的所有学生
        List<SysUser> students = userMapper.selectList(
                new QueryWrapper<SysUser>().eq("class_id", classId).eq("role", "STUDENT")
        );

        if (students.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 提取学生 ID 列表
        List<Long> studentIds = students.stream().map(SysUser::getId).collect(Collectors.toList());

        // 建一个 Map，方便后面快速根据 ID 找学生信息
        Map<Long, SysUser> studentMap = students.stream()
                .collect(Collectors.toMap(SysUser::getId, s -> s));

        // 第二步：根据这些学生 ID 查他们的提交记录
        List<EssaySubmission> list = submissionMapper.selectList(
                new QueryWrapper<EssaySubmission>()
                        .in("student_id", studentIds)
                        .orderByDesc("submit_time")
        );

        // 第三步：组装数据，把名字、学号、任务标题填进去
        for (EssaySubmission sub : list) {
            // 1. 填学生姓名和学号
            SysUser stu = studentMap.get(sub.getStudentId());
            if (stu != null) {
                sub.setStudentName(stu.getName());
                sub.setStudentNo(stu.getStudentId());
            }

            // 2. 填任务标题
            EssayTask task = taskMapper.selectById(sub.getTaskId());
            if (task != null) {
                sub.setTaskTitle(task.getTitle());
            }

            // 🔥 3. 新增：去历史表里实时查询修改次数并赋值
            Long historyCount = historyMapper.selectCount(
                    new QueryWrapper<EssayRevisionHistory>().eq("submission_id", sub.getId())
            );
            sub.setRevisionCount(historyCount.intValue());
        }

        return Result.success(list);
    }

    // 2. 获取某次提交的对话记录
    @GetMapping("/messages/{sessionId}")
    public Result getChatMessages(@PathVariable Long sessionId) {
        List<ChatMessage> messages = chatMessageMapper.selectList(
                new QueryWrapper<ChatMessage>().eq("session_id", sessionId).orderByAsc("created_at")
        );
        return Result.success(messages);
    }

    // 3. 提交批阅（支持阈值判定和强制通过）
    @PostMapping("/submit")
    public Result submitGrade(@RequestBody Map<String, Object> request) {
        try {
            Long submissionId = Long.valueOf(request.get("submissionId").toString());
            Integer score = Integer.valueOf(request.get("score").toString());
            String teacherComment = request.get("teacherComment").toString();
            // 🔥 默认及格线 60，默认允许打回
            int passScore = request.containsKey("passScore") ? Integer.parseInt(request.get("passScore").toString()) : 60;
            boolean forcePass = request.containsKey("forcePass") && (boolean)request.get("forcePass");

            EssaySubmission submission = submissionMapper.selectById(submissionId);
            if (submission == null) {
                return Result.error("找不到该作文记录");
            }

            submission.setScore(score);
            submission.setTeacherComment(teacherComment);
            submission.setGradeTime(LocalDateTime.now());

            // 🔥 核心判断：如果分数小于阈值，且老师没有强制通过，则打回重做
            if (score < passScore && !forcePass) {
                submission.setStatus("NEED_REVISE"); // 打回重做
            } else {
                submission.setStatus("GRADED"); // 批阅通过
            }

            submissionMapper.updateById(submission);
            return Result.success(score < passScore && !forcePass ? "已打回重做" : "批阅成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("批阅失败：" + e.getMessage());
        }
    }

    // 获取某篇作文的所有修改历史
    @GetMapping("/history/{submissionId}")
    public Result getHistory(@PathVariable Long submissionId) {
        List<EssayRevisionHistory> list = historyMapper.selectList(
                new QueryWrapper<EssayRevisionHistory>()
                        .eq("submission_id", submissionId)
                        .orderByAsc("revision_count")
        );
        return Result.success(list);
    }
}