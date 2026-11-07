package com.example.dto;

import lombok.Data;
import java.util.List;
//用于接收前端传来的 themeId 和勾选的班级 ID 列表
@Data
public class PublishThemeDTO {
    private Long themeId;
    private List<Long> classIds;
}