package com.cy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户会话（一次识别流程）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("session")
public class Session {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 餐次类型（BREAKFAST/LUNCH/DINNER/SNACK/CUSTOM） */
    private String mealType;

    /** 自定义餐次名称 */
    private String customMealName;

    /** 整个会话的聚合营养 */
    private Double totalCalories;
    private Double protein;
    private Double fat;
    private Double carbs;

    /** 状态（INIT/DONE） */
    private String status;

    private LocalDateTime createTime;
}