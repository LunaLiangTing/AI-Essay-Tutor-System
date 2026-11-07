package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.Result;
import com.example.entity.EssaySubmission;
import com.example.entity.SysUser;
import com.example.entity.TClass;
import com.example.mapper.ClassMapper;
import com.example.mapper.EssaySubmissionMapper;
import com.example.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher/dashboard")
public class TeacherDashboardController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ClassMapper classMapper;
    @Autowired
    private EssaySubmissionMapper submissionMapper;

    @GetMapping("/stats")
    public Result getDashboardStats(@RequestAttribute("userId") Long teacherId) {
        try {
            Map<String, Object> data = new HashMap<>();

            // 1. 查询该教师的所有班级
            List<TClass> classes = classMapper.selectList(
                    new QueryWrapper<TClass>().eq("teacher_id", teacherId)
            );
            List<Long> classIds = classes.stream().map(TClass::getId).collect(Collectors.toList());

            // 2. 统计数据
            long totalClasses = classes.size();
            long totalStudents = 0;
            long totalSubmissions = 0;
            long pendingReview = 0;
            long graded = 0;
            long needRevise = 0;

            // 3. 近7天趋势数据
            List<String> trendDates = new ArrayList<>();
            List<Long> trendCounts = new ArrayList<>();
            Map<String, Long> dailyCountMap = new HashMap<>();

            // 初始化近7天日期
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                String dateStr = date.format(formatter);
                trendDates.add(dateStr);
                dailyCountMap.put(dateStr, 0L);
            }

            if (!classIds.isEmpty()) {
                List<SysUser> students = userMapper.selectList(
                        new QueryWrapper<SysUser>().in("class_id", classIds).eq("role", "STUDENT")
                );

                if (!students.isEmpty()) {
                    List<Long> studentIds = students.stream().map(SysUser::getId).collect(Collectors.toList());
                    List<EssaySubmission> submissions = submissionMapper.selectList(
                            new QueryWrapper<EssaySubmission>().in("student_id", studentIds)
                    );

                    totalSubmissions = submissions.size();
                    for (EssaySubmission sub : submissions) {
                        if ("SUBMITTED".equals(sub.getStatus())) pendingReview++;
                        else if ("GRADED".equals(sub.getStatus())) graded++;
                        else if ("NEED_REVISE".equals(sub.getStatus())) needRevise++;

                        // 统计近7天提交量
                        if (sub.getSubmitTime() != null) {
                            String subDate = sub.getSubmitTime().format(formatter);
                            if (dailyCountMap.containsKey(subDate)) {
                                dailyCountMap.put(subDate, dailyCountMap.get(subDate) + 1);
                            }
                        }
                    }
                }
            }

            // 组装趋势数据
            for (String date : trendDates) {
                trendCounts.add(dailyCountMap.get(date));
            }

            data.put("totalClasses", totalClasses);
            data.put("totalStudents", totalStudents);
            data.put("totalSubmissions", totalSubmissions);
            data.put("pendingReview", pendingReview);
            data.put("graded", graded);
            data.put("needRevise", needRevise);
            data.put("trendDates", trendDates);
            data.put("trendCounts", trendCounts);

            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取数据失败：" + e.getMessage());
        }
    }
}