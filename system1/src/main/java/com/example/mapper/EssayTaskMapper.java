// 1. EssayTaskMapper.java
package com.example.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.EssayTask;
import org.apache.ibatis.annotations.Mapper;
//学生端的essay_task（任务）
@Mapper
public interface EssayTaskMapper extends BaseMapper<EssayTask> {}

