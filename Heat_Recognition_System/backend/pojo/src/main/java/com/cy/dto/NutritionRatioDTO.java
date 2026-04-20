package com.cy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 营养比例（百分比）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NutritionRatioDTO {
	private Double proteinRatio;
	private Double fatRatio;
	private Double carbsRatio;
}

