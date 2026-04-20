package com.cy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 热量趋势点
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CaloriesTrendPoint {
	private String date;
	private Double calories;
}

