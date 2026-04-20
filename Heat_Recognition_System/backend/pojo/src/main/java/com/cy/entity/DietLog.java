package com.cy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 饮食日志（每日/每餐汇总）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DietLog {

    private Long id;

    private Long userId;

    /** 日期 */
    private LocalDate date;

    /** 餐次 */
//    private String mealType;

    /** 营养汇总 */
    private Double totalCalories;
    private Double protein;
    private Double fat;
    private Double carbs;

    private LocalDateTime createTime;
}