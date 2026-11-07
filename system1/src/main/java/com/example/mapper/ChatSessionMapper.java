// 2. ChatSessionMapper.java
package com.example.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
//学生chat_session（会话）
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {}