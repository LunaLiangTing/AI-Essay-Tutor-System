package com.example.dto;

import lombok.Data;

@Data
public class CreateClassDTO {
    // 班级名称 (如：四年级一班)
    private String className;

    // 班级人数
    private Integer studentCount;

    // 备注
    private String remark;
}