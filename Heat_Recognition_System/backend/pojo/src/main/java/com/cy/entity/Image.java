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
 * 图片及营养分析结果（核心表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("image")
public class Image {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话ID */
    private Long sessionId;

    /** 图片地址 */
    private String imageUrl;

    /** 聚合营养信息 */
    private Double totalCalories;
    private Double protein;
    private Double fat;
    private Double carbs;

    /** 用户补充信息 */
    private String extraInfo;

    /** AI提示用户补充内容 */
    private String aiPromptHint;

    /** AI首次识别JSON */
    private String aiRawResult;

    /** AI二次识别JSON */
    private String aiRefinedResult;

    /** 是否已优化 */
    private Integer isRefined;

    /** 数据来源（AI/USER） */
    private String sourceType;

    private LocalDateTime createTime;
}