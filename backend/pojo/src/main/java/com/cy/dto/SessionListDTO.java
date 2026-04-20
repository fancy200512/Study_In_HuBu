package com.cy.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表项
 */
@Data
public class SessionListDTO {
	private Long id;
	private String mealType;
	private Double totalCalories;
	private LocalDateTime createTime;
}

