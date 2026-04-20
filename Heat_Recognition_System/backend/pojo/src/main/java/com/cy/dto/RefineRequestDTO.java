package com.cy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 二次识别请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefineRequestDTO {

    private Long sessionId;

    private Long imageId;

    /** 用户补充信息 */
    private String extraInfo;
}