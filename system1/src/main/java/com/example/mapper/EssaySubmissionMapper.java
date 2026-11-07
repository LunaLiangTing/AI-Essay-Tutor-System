// 4. EssaySubmissionMapper.java
package com.example.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.EssaySubmission;
import org.apache.ibatis.annotations.Mapper;
//学生essay_submission（提交记录）
@Mapper
public interface EssaySubmissionMapper extends BaseMapper<EssaySubmission> {}