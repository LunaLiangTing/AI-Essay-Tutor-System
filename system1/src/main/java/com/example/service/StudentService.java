package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.entity.*;
import com.example.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//学生端的所有业务
@Service
public class StudentService {

    @Autowired
    private EssayTaskMapper essayTaskMapper;
    @Autowired
    private ChatSessionMapper chatSessionMapper;
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private EssaySubmissionMapper essaySubmissionMapper;
    @Autowired
    private UserMapper userMapper;
//    由于需要查“主题-班级”关联表，所以这里注入了ThemeClassMapper和EssayThemeMapper
    @Autowired
    private ThemeClassMapper themeClassMapper;
    @Autowired
    private EssayThemeMapper essayThemeMapper;
    //用于与学生提交的作文的历史版本的数据表进行CRDU
    @Autowired
    private EssayRevisionHistoryMapper historyMapper;
    // 1. 获取该学生所在班级的所有写作任务（支持红点提示），获取该学生所在班级发布的主题和任务（嵌套结构）
    public List<Map<String, Object>> getMyTasks(Long studentId) {
        // 1. 查学生信息，拿到班级 ID
        SysUser student = userMapper.selectById(studentId);
        if (student == null || student.getClassId() == null) {
            throw new RuntimeException("你还没有加入班级，请联系老师获取班级码");
        }

        // 2. 查这个班级被发布了哪些主题 (theme_class 表)
        List<ThemeClass> themeClasses = themeClassMapper.selectList(
                new QueryWrapper<ThemeClass>().eq("class_id", student.getClassId())
        );
        if (themeClasses.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> themeIds = themeClasses.stream().map(ThemeClass::getThemeId).collect(Collectors.toList());

        // 3. 根据主题 ID 查具体的主题信息
        List<EssayTheme> themes = essayThemeMapper.selectBatchIds(themeIds);

        // 4. 组装数据（每个主题下带着它的任务列表）
        List<Map<String, Object>> result = new ArrayList<>();
        for (EssayTheme theme : themes) {
            Map<String, Object> map = new HashMap<>();
            map.put("themeId", theme.getId());
            map.put("themeName", theme.getThemeName());
            map.put("teachingObjective", theme.getTeachingObjective());

            // 查这个主题下的所有任务
            List<EssayTask> tasks = essayTaskMapper.selectList(
                    new QueryWrapper<EssayTask>().eq("theme_id", theme.getId()).orderByAsc("id")
            );
            map.put("tasks", tasks);
            result.add(map);
        }
        return result;
    }

    // 2. 获取任务详情（给 Study.vue 提供图片和 Prompt）
    public EssayTask getTaskDetail(Long taskId) {
        EssayTask task = essayTaskMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("任务不存在");
        return task;
    }

    // 3. 开启写作会话
    public ChatSession startSession(Long studentId, Long taskId) {
        // 检查是否已有进行中的会话
        QueryWrapper<ChatSession> wrapper = new QueryWrapper<>();
        wrapper.eq("student_id", studentId).eq("task_id", taskId).eq("status", "IN_PROGRESS");
        ChatSession exist = chatSessionMapper.selectOne(wrapper);
        if (exist != null) return exist;

        ChatSession session = new ChatSession();
        session.setStudentId(studentId);
        session.setTaskId(taskId);
        session.setStartTime(LocalDateTime.now());
        session.setStatus("IN_PROGRESS");
        chatSessionMapper.insert(session);
        return session;
    }

    // 4. 保存对话记录（重要：用于过程性评价）
    public void saveMessage(Long sessionId, String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(msg);
    }

    // 5. 提交最终作文
    public void submitEssay(EssaySubmission submission) {
        // 校验会话是否存在
        ChatSession session = chatSessionMapper.selectById(submission.getSessionId());
        if (session == null) throw new RuntimeException("会话不存在");

        // 更新会话状态
        if (session != null) {
            session.setStatus("SUBMITTED");
            chatSessionMapper.updateById(session);
        }

        // 保存作文记录
        submission.setStatus("SUBMITTED");
        submission.setSubmitTime(LocalDateTime.now());
        essaySubmissionMapper.insert(submission);
    }

    // 6. 查看我的历史作文列表
    // 6. 查看我的历史作文列表
    public List<EssaySubmission> getMyEssays(Long studentId) {
        List<EssaySubmission> list = essaySubmissionMapper.selectList(
                new QueryWrapper<EssaySubmission>()
                        .eq("student_id", studentId)
                        .orderByDesc("submit_time")
        );

        for (EssaySubmission sub : list) {
            // 关联查询任务名称
            EssayTask task = essayTaskMapper.selectById(sub.getTaskId());
            if (task != null) {
                sub.setTaskTitle(task.getTitle());
            }

            // 🔥 动态计算次数：去历史表里 COUNT 一下
            Long count = historyMapper.selectCount(
                    new QueryWrapper<EssayRevisionHistory>().eq("submission_id", sub.getId())
            );
            sub.setRevisionCount(count.intValue());
        }
        return list;
    }

    // 7. 查看作文详情（含老师评语，用于二次修改）
    public EssaySubmission getEssayDetail(Long submissionId) {
        return essaySubmissionMapper.selectById(submissionId);
    }

    // 8. 提交再次修改的作文
    // 8. 提交二次修改的作文
    public void reviseEssay(Long submissionId, String revisedEssay, String revisionReflection) {
        EssaySubmission submission = essaySubmissionMapper.selectById(submissionId);
        if (submission == null) throw new RuntimeException("作文不存在");

        // 🔥 如果当前是已经批阅或者需要重做，肯定能改
        // 如果是刚提交（SUBMITTED）或者修改了还在等批阅（REVISED），也允许他继续改
        if ("GRADED".equals(submission.getStatus()) ||
                "NEED_REVISE".equals(submission.getStatus()) ||
                "SUBMITTED".equals(submission.getStatus()) ||
                "REVISED".equals(submission.getStatus())) {
            // 放行
        } else {
            throw new RuntimeException("当前状态暂不能进行修改");
        }

        // 🔥 核心新增：把这次提交的内容存入历史表
        // 先查一下现在是第几次了
        Long historyCount = historyMapper.selectCount(
                new QueryWrapper<EssayRevisionHistory>().eq("submission_id", submissionId)
        );
        int currentCount = historyCount.intValue() + 1;

        EssayRevisionHistory history = new EssayRevisionHistory();
        history.setSubmissionId(submissionId);
        history.setRevisionCount(currentCount);
        history.setEssayContent(revisedEssay);
        history.setRevisionReflection(revisionReflection);
        history.setCreatedAt(java.time.LocalDateTime.now());
        historyMapper.insert(history);

        // 更新主表的最新状态（这里不需要再存次数了）
        submission.setRevisedEssay(revisedEssay);
        submission.setRevisionReflection(revisionReflection);
        submission.setStatus("REVISED");
        submission.setRevisionTime(java.time.LocalDateTime.now());
        essaySubmissionMapper.updateById(submission);
    }
}