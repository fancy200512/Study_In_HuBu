package com.cy.dto;

import lombok.Data;

/**
 * 二次识别请求
 */
@Data
public class RecognitionRefineRequest {
	private Long sessionId;
	private Long imageId;
	private String extraInfo;
}

