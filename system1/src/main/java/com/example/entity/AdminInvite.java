package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("admin_invite")
public class AdminInvite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String inviteCode;
    private Boolean isUsed;
    private Long creatorId;
    private LocalDateTime createdAt;
}