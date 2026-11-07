package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("essay_revision_history")
public class EssayRevisionHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Integer revisionCount;
    private String essayContent;
    private String revisionReflection;
    private LocalDateTime createdAt;
}