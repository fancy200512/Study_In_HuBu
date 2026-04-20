package com.cy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户身体数据表（健康核心）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBodyProfile {

    private Long id;

    private Long userId;

    /** 身高(cm) */
    private Double height;

    /** 体重(kg) */
    private Double weight;

    /** 体脂率(%) */
    private Double bodyFat;

    /** 年龄 */
    private Integer age;

    /** BMI（系统计算） */
    private Double bmi;

    /** BMR基础代谢（系统计算） */
    private Double bmr;

    /** 目标热量 */
    private Double targetCalories;

    private LocalDateTime updateTime;
}