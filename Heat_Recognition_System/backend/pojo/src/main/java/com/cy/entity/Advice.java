package com.cy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 饮食建议表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Advice {

    private Long id;

    private Long userId;

    /** 对应日期 */
    private LocalDate relatedDate;

    /** 建议内容 */
    private String content;

    /** 类型（CALORIES/NUTRITION） */
    private String type;

    private LocalDateTime createTime;
}